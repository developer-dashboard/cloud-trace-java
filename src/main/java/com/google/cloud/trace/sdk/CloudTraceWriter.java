// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.trace.sdk;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Writes traces to the public Google Cloud Trace API.
 */
public class CloudTraceWriter implements TraceWriter, CanInitFromProperties {

  /**
   * The scope(s) we need to write traces to the Cloud Trace API.
   */
  public static List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/trace.append");

  /**
   * Request factory for calling the Cloud Trace API.
   */
  private HttpRequestFactory requestFactory;

  /**
   * The id of the cloud project to write traces to.
   */
  private String projectId;

  /**
   * The endpoint of the Google API service to call.
   */
  private String apiEndpoint = "https://www.googleapis.com/";

  /** Instance of the HTTP transport. */
  private HttpTransport httpTransport;

  /**
   * JSON mapper for forming API requests.
   */
  private ObjectMapper objectMapper;

  public CloudTraceWriter() throws GeneralSecurityException, IOException {
    this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    this.objectMapper = new ObjectMapper();
    this.requestFactory = this.httpTransport.createRequestFactory();
  }

  public HttpRequestFactory getRequestFactory() {
    return requestFactory;
  }

  public void setRequestFactory(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getApiEndpoint() {
    return apiEndpoint;
  }

  public void setApiEndpoint(String apiEndpoint) {
    this.apiEndpoint = apiEndpoint;
  }

  @Override
  public void initFromProperties(Properties props) throws CloudTraceException {
    this.projectId = props.getProperty(getClass().getName() + ".projectId");
    this.apiEndpoint = props.getProperty(getClass().getName() + ".apiEndpoint");
    String credentialProviderClassName =
        props.getProperty(getClass().getName() + ".credentialProvider");
    if (credentialProviderClassName != null && !credentialProviderClassName.isEmpty()) {
      CredentialProvider credProvider = (CredentialProvider) ReflectionUtils.createFromProperties(
          credentialProviderClassName, props);
      this.requestFactory = this.httpTransport.createRequestFactory(credProvider.getCredential());
    }
  }

  @Override
  public void writeSpan(TraceSpanData span) throws CloudTraceException {
    checkState();
    GenericUrl url = buildUrl(span);
    try {
      Map<String, String> patchData = new HashMap<>();
      patchData.put("traceId", span.getTraceId());
      patchData.put("projectId", projectId);
      String requestBody = objectMapper.writeValueAsString(patchData);

      HttpRequest request =
          requestFactory.buildPatchRequest(url, ByteArrayContent.fromString(null, requestBody));
      request.getHeaders().setContentType("application/json");
      HttpResponse response = request.execute();
      if (response.getStatusCode() != HttpStatusCodes.STATUS_CODE_OK) {
        throw new CloudTraceException(
            "Failed to write span, status = " + response.getStatusCode());
      }
    } catch (IOException e) {
      throw new CloudTraceException("Exception writing span to API, url=" + url, e);
    }
  }

  /**
   * Creates the URL to use to patch the trace with the given span.
   */
  private GenericUrl buildUrl(TraceSpanData span) {
    String url = apiEndpoint + "v1/projects/" + projectId + "/traces/" + span.getTraceId();
    return new GenericUrl(url);
  }

  @Override
  public void writeSpans(List<TraceSpanData> spans) throws CloudTraceException {
    // TODO: Actually roll the batch into a single append call.
    for (TraceSpanData span : spans) {
      writeSpan(span);
    }
  }

  @Override
  public void shutdown() {}

  /**
   * Validates the state before attempting to write a trace to the API.
   */
  private void checkState() {
    if (projectId == null || projectId.isEmpty()) {
      throw new IllegalStateException("Project id must be set");
    }
    if (apiEndpoint == null || apiEndpoint.isEmpty()) {
      throw new IllegalStateException("API endpoint must be set");
    }
  }
}
