/*
 * Copyright (c) 2015 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.nav.plugin.client;

import com.cloudera.com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudera.nav.plugin.model.Source;
import com.cloudera.nav.plugin.model.SourceType;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * An API client to communicate with Navigator to register and validate
 * metadata models
 */
public class NavApiCient {

  private static final Logger LOG = LoggerFactory.getLogger(NavApiCient.class);
  private static final String SOURCE_QUERY = "type:SOURCE";
  private static final String ALL_QUERY = "type:*";

  private final PluginConfigurations config;
  private final Cache<String, Source> sourceCacheByUrl;
  private final Cache<SourceType, Collection<Source>> sourceCacheByType;

  public NavApiCient(PluginConfigurations config) {
    this.config = config;
    sourceCacheByUrl = CacheBuilder.newBuilder().build();
    sourceCacheByType = CacheBuilder.newBuilder().build();
  }

  /**
   * Registers a given set of metadata models
   * @param models
   */
  public void registerModels(Collection<Object> models) {
    throw new UnsupportedOperationException("no yet implemented");
  }

  /**
   * Call the Navigator API and retrieve all available sources
   *
   * @return a collection of available sources
   */
  public Collection<Source> getAllSources() {
    RestTemplate restTemplate = new RestTemplate();
    String url = getUrl();
    HttpHeaders headers = getAuthHeaders();
    HttpEntity<String> request = new HttpEntity<String>(headers);
    ResponseEntity<SourceAttrs[]> response = restTemplate.exchange(url,
        HttpMethod.GET, request, SourceAttrs[].class);
    Collection<Source> sources = Lists.newArrayList();
    for (SourceAttrs info : response.getBody()) {
      sources.add(info.createSource());
    }
    return sources;
  }
  /** Returns all of the entities and relations in the database, plus a marker to denote when this search took place
   *
   * @return
   */
  public Iterable<Map<String, Object>> getAllUpdated(){
    Map<String, Integer> newMarker = getNewMarker();
    String marker= newMarker.toString(); // JSON --use Jackson
    Map<String, Object>  entitiesUpdated;
    Map<String, Object>  relationsUpdated;
    Iterable<Map<String, Object>> updatedResults;
    //TODO
    return updatedResults;
  }

  /**Returns all of the entities and relations in the database that have been updated or added since the source iterations indicated by the marker
   *
   * @param markerRep JSON representation of sourceId : sourceExtractorIteration
   * @return
   */
  public Iterable<Map<String, Object>> getAllUpdated(String markerRep){
    try {
      Map<String, Integer> marker = new ObjectMapper().readValue(markerRep, HashMap.class);
      Map<String, Integer> newMarker = getNewMarker();
      ResponseEntity<String> entityResponse = navResponse("entities", marker, newMarker);
      //TODO

    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    //incorrect exception handling
    return new ArrayList<Map<String, Object>>();
  }


  public ResponseEntity<String> navResponse(String type, Map marker1, Map marker2){
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = getAuthHeaders();
    HttpEntity<String> request =new HttpEntity<String>(headers);
    String extractorQuery = getExtractorQueryString(marker1, marker2);
    String url = getUrl(type, extractorQuery);;
    ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, request);
    return response;
  }

  /**
   *
   * @return
   */
  public Map<String, Integer> getNewMarker(){
    Collection<Source> sources = getAllSources(); // Replace with sourceCacheByUrl and loadAllSources ?
    HashMap<String, Integer> newMarker = new HashMap<String, Integer>();
    for (Source source : sources){
      String id = source.getIdentity();
      Integer extractorIteration = source.getExtractorIteration();
      newMarker.put(id, extractorIteration);
    }
    return newMarker;
  }


  /**
   * Get the Source corresponding to the Hadoop service Url from Navigator.
   * A NoSuchElementException is thrown if the url does not correspond to
   * any known Source
   *
   * @param serviceUrl
   * @return
   */
  public Source getSourceForUrl(String serviceUrl) {
    Source source = sourceCacheByUrl.getIfPresent(serviceUrl);
    if (source == null) {
      loadAllSources();
    }
    source = sourceCacheByUrl.getIfPresent(serviceUrl);
    Preconditions.checkArgument(source != null,
        "Could not find Source at " + serviceUrl);
    return source;
  }

  /**
   * Return the only Source of the given type, throw exception if more than
   * one is found.
   *
   * @param sourceType
   * @return
   */
  public Source getOnlySource(SourceType sourceType) {
    Collection<Source> sources = getSourcesForType(sourceType);
    Preconditions.checkNotNull(sources, "Could not find sources for " +
        "source type " + sourceType.name());
    return Iterables.getOnlyElement(sources);
  }

  public Collection<Source> getSourcesForType(SourceType sourceType) {
    Collection<Source> sources = sourceCacheByType.getIfPresent(sourceType);
    if (sources == null) {
      loadAllSources();
      sources = sourceCacheByType.getIfPresent(sourceType);
    }
    return sources;
  }

  /**
   * Clear the cache of Sources that have been previously loaded.
   */
  public void resetSources() {
    sourceCacheByUrl.invalidateAll();
    sourceCacheByType.invalidateAll();
  }

  private HttpHeaders getAuthHeaders() {
    // basic authentication with base64 encoding
    String plainCreds = String.format("%s:%s", config.getUsername(),
        config.getPassword());
    byte[] plainCredsBytes = plainCreds.getBytes();
    byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
    String base64Creds = new String(base64CredsBytes);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + base64Creds);
    return headers;
  }


  private String getUrl() {
    // form the url string to request all entities with type equal to SOURCE
    String baseNavigatorUrl = config.getNavigatorUrl();
    String entities = joinUrlPath(baseNavigatorUrl, "entities");
    return String.format("%s?query=%s", entities, SOURCE_QUERY);
  }

  /**
   *
   * @param type "entities", "relations"
   * @return
   */
  private String getUrl(String type, String queryString) {
    String baseNavigatorUrl = config.getNavigatorUrl();
    String typeUrl = joinUrlPath(baseNavigatorUrl, type);
    return String.format("%s?query=?%s", typeUrl, queryString);
  }

  private String joinUrlPath(String base, String component) {
    return base + (base.endsWith("/") ? "" : "/") + component;
  }

  private String getExtractorQueryString(Map<String, Integer> m1, Map<String, Integer> m2){
    HashSet<String> possibleExtractorRunIds = new HashSet<String>();
    String queryString = "extractorRunId:(";
    for (String key : m1.keySet()){
      for (int i=m1.get(key); i==m2.get(key); i++){
        String possible = key + "##" + Integer.toString(i);
        queryString = queryString + possible + " OR ";
      }
    }
    return queryString.substring(0,-4)+")";
  }

  private void loadAllSources() {
    Collection<Source> allSources = getAllSources();
    for (Source source : allSources) {
      if (source.getSourceUrl() == null) {
        LOG.warn(String.format("Source %s did not have a source url",
            source.getName() != null ? source.getName() :
                source.getIdentity()));
        continue;
      }
      sourceCacheByUrl.put(source.getSourceUrl(), source);
      try {
        Collection<Source> forType = sourceCacheByType.get(
            source.getSourceType(),
            new Callable<Collection<Source>>() {
              @Override
              public Collection<Source> call() throws Exception {
                return Sets.newHashSet();
              }
            });
        forType.add(source);
      } catch (ExecutionException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
