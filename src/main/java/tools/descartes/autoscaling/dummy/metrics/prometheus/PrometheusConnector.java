package tools.descartes.autoscaling.dummy.metrics.prometheus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.descartes.autoscaling.dummy.metrics.RuntimeDataImporter;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.metrics.RewritableMetric;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A RuntimeDataImporter which queries monitoring data from Prometheus
 */
public class PrometheusConnector implements RuntimeDataImporter {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusConnector.class);

    private final String prometheusUrl;
    private final CloseableHttpClient httpClient;
    private final MetricAggregator aggregator;

    public PrometheusConnector(String prometheusUrl, MetricAggregator aggregator) {
        this.prometheusUrl = prometheusUrl;
        httpClient = HttpClients.createDefault();
        this.aggregator = aggregator;
    }

    private <T> T performGetRequest(String url, Class<T> targetClass) throws JsonProcessingException {
        HttpGet request = new HttpGet(prometheusUrl + url);
        String text;
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            text = EntityUtils.toString(entity);
        } catch (ClientProtocolException cpe) {
            logger.error("ClientProtocolException while querying Prometheus server at " + prometheusUrl);
            return null;
        } catch (IOException ioe) {
            logger.error("IOException while querying from Prometheus server at " + prometheusUrl);
            return null;
        }
        return new ObjectMapper().readValue(text, targetClass);
    }

    @Override
    public List<String> getAllMetricNames() {
        GetAllMetricsResponse resp = null;
        try {
             resp = performGetRequest("/api/v1/label/__name__/values", GetAllMetricsResponse.class);
        } catch (JsonProcessingException jpe) {
            logger.error("Error while querying metric names from Prometheus", jpe);
        }
        if (resp == null) return new ArrayList<>();
        if (!resp.status.equals("success")) {
            logger.error("Querying Prometheus metric names returned status " + resp.status);
            return new ArrayList<>();
        }
        return resp.data;
    }

    @Override
    public List<Metric> getAveragedValuesForMetric(String metricName, int windowInSeconds) {
        MatrixResult result;
        try {
            result = performGetRequest("/api/v1/query?query=" + metricName + "[" + windowInSeconds + "s]", MatrixResult.class);
        } catch (JsonProcessingException jpe) {
            logger.error("Querying results for metric " + metricName + " resulted in JsonProcessingException", jpe);
            return new ArrayList<>();
        }
        if (result == null) return new ArrayList<>();
        if (!result.status.equals("success")) {
            logger.error("Querying metric " + metricName + " returned status " + result.status);
            return new ArrayList<>();
        }
        return aggregator.aggregateMetrics(result);
    }

    @Override
    public RewritableMetric getSingleMetric(String query) {
        VectorResult result;
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            result = performGetRequest("/api/v1/query?query=" + encodedQuery, VectorResult.class);
        } catch (UnsupportedEncodingException usee) {
            logger.error("UnsupportedEncodingException: ", usee);
            return null;
        } catch (JsonProcessingException jpe) {
            logger.error("Querying results for metric " + query + " resulted in JsonProcessingException", jpe);
            return null;
        }
        if (result == null) return null;
        if (!result.status.equals("success")) {
            logger.error("Querying metric " + query + " returned status " + result.status);
            return null;
        }
        return aggregator.aggregateMetrics(result);
    }

    /*

    public List<PrometheusData> getCurrentMetricValue(String metricName) {
        return getCurrentMetricValue(metricName, null, null);
    }

    public List<PrometheusData> getCurrentMetricValue(String metricName, Map<String, String> labelConfig) {
        return getCurrentMetricValue(metricName, labelConfig, null);
    }

    public List<PrometheusData> getCurrentMetricValue(String metricName, Map<String, String> labelConfig, Map<String, String> params) {
        String query = getQuery(metricName, labelConfig);
        return customQuery(query, params);
    }

    public List<PrometheusData> getMetricRangeData(String metricName) {
        long currentTime = System.currentTimeMillis() / 1000L;
        return getMetricRangeData(metricName, null, currentTime - 600, currentTime, 600, null);
    }

    public List<PrometheusData> getMetricRangeData(String metricName, Map<String, String> labelConfig) {
        long currentTime = System.currentTimeMillis() / 1000L;
        return getMetricRangeData(metricName, labelConfig, currentTime - 600, currentTime, 600, null);
    }

    public List<PrometheusData> getMetricRangeData(String metricName, Map<String, String> labelConfig, Map<String, String> params) {
        long currentTime = System.currentTimeMillis() / 1000L;
        return getMetricRangeData(metricName, labelConfig, currentTime - 600, currentTime, 600, params);
    }

    public List<PrometheusData> getMetricRangeData(String metricName, Map<String, String> labelConfig, long start, long end) {
        return getMetricRangeData(metricName, labelConfig, start, end, end-start, null);
    }

    public List<PrometheusData> getMetricRangeData(String metricName, long start, long end) {
        return getMetricRangeData(metricName, null, start, end, end-start, null);
    }

    public List<PrometheusData> getMetricRangeData(String metricName, long start, long end, long chunkSize) {
        return getMetricRangeData(metricName, null, start, end, chunkSize, null);
    }

    public List<PrometheusData> getMetricRangeData(String metricName, Map<String, String> labelConfig, long start, long end, long chunkSize) {
        return getMetricRangeData(metricName, labelConfig, start, end, chunkSize, null);
    }

    public List<PrometheusData> getMetricRangeData(String metricName, Map<String, String> labelConfig, long start, long end, long chunkSize, Map<String, String> params) {
        if (end - start < chunkSize) {
            throw new IllegalArgumentException("Chunk size too large");
        }
        String query = getQuery(metricName, labelConfig);
        List<PrometheusData> result = new ArrayList<>();
        while (start < end) {
            if (start + chunkSize > end) {
                chunkSize = end - start;
            }
            URI uri;
            try {
                URIBuilder uriBuilder = new URIBuilder(prometheusUrl + "/api/v1/query");
                uriBuilder.addParameter("query", query + "[" + chunkSize + "s]");
                uriBuilder.addParameter("time", Long.toString(start + chunkSize));
                if (params != null) {
                    params.keySet().forEach(key -> uriBuilder.addParameter(key, params.get(key)));
                }
                uri = uriBuilder.build();
            } catch (URISyntaxException use) {
                use.printStackTrace(System.err);
                return null;
            }
            String text = sendRequest(uri);
            if (text == null) return null;
            JSONArray data = new JSONObject(text).getJSONObject("data").getJSONArray("result");
            result.addAll(PrometheusData.fromJSONArray(data));
            start += chunkSize;
        }
        return result;
    }

    public List<PrometheusData> customQuery(String query) {
        return customQuery(query, null);
    }

    public List<PrometheusData> customQuery(String query, Map<String, String> params) {
        URI uri;
        try {
            URIBuilder uriBuilder = new URIBuilder(prometheusUrl + "/api/v1/query");
            uriBuilder.addParameter("query", query);
            if (params != null) {
                params.keySet().forEach(key -> uriBuilder.addParameter(key, params.get(key)));
            }
            uri = uriBuilder.build();
        } catch (URISyntaxException use) {
            use.printStackTrace(System.err);
            return null;
        }
        String text = sendRequest(uri);
        if (text == null) return null;
        try {
            JSONArray data = new JSONObject(text).getJSONObject("data").getJSONArray("result");
            return PrometheusData.fromJSONArray(data);
        } catch (JSONException je) {
            logger.error("Query failed: " + query + ", reason: " + je.getMessage());
        }
        return null;
    }

    public List<PrometheusRangeData> customQueryRange(String query, long start, long end, String step) {
        return customQueryRange(query, start, end, step, null);
    }

    public List<PrometheusRangeData> customQueryRange(String query, long start, long end, String step, Map<String, String> params) {
        URI uri;
        try {
            URIBuilder uriBuilder = new URIBuilder(prometheusUrl + "/api/v1/query_range");
            uriBuilder.addParameter("query", query);
            uriBuilder.addParameter("start", Long.toString(start));
            uriBuilder.addParameter("end", Long.toString(end));
            uriBuilder.addParameter("step", step);
            if (params != null) {
                params.keySet().forEach(key -> uriBuilder.addParameter(key, params.get(key)));
            }
            uri = uriBuilder.build();
        } catch (URISyntaxException use) {
            use.printStackTrace(System.err);
            return null;
        }
        String text = sendRequest(uri);
        if (text == null) return null;
        JSONArray data = new JSONObject(text).getJSONObject("data").getJSONArray("result");
        return PrometheusRangeData.fromJSONArray(data);
    }

    public Map<String, Double> getMetricAggregation(String query, List<String> operations) {
        return getMetricAggregation(query, operations, -1, -1, "15", null);
    }

    public Map<String, Double> getMetricAggregation(String query, List<String> operations, long start, long end) {
        return getMetricAggregation(query, operations, start, end, "15", null);
    }

    public Map<String, Double> getMetricAggregation(String query, List<String> operations, long start, long end, String step) {
        return getMetricAggregation(query, operations, start, end, step, null);
    }

    public Map<String, Double> getMetricAggregation(String query, List<String> operations, long start, long end, String step, Map<String, String> params) {
        if (operations.size() == 0) {
            throw new IllegalArgumentException("size of operations cannot be zero");
        }
        List<Double> queryValues = new ArrayList<>();
        if (start != -1 && end != -1) {
            List<PrometheusRangeData> data = customQueryRange(query, start, end, step, params);
            for (PrometheusRangeData prd : data) {
                prd.getValues().forEach(pv -> {
                    if (!pv.isNumericValue()) {
                        throw new IllegalArgumentException("Not a numeric value metric");
                    }
                    queryValues.add(pv.getNumericValue());
                });
            }
        } else {
            List<PrometheusData> data = customQuery(query, params);
            for (PrometheusData pd : data) {
                if (!pd.getPrometheusValue().isNumericValue()) {
                    throw new IllegalArgumentException("Not a numeric value metric");
                }
                queryValues.add(pd.getPrometheusValue().getNumericValue());
            }
        }

        Map<String, Double> result = new HashMap<>();
        if (queryValues.isEmpty()) return result;

        for (String op : operations) {
            if (op.startsWith("percentile")) {
                double p = Double.parseDouble(op.split("_")[1]);
                Percentile pct = new Percentile();
                double[] unboxed = Stream.of(queryValues.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                result.put(op, pct.evaluate(unboxed, p));
                continue;
            }
            switch (op) {
                case "sum":
                    result.put("sum", queryValues.stream().mapToDouble(d -> d).sum());
                    break;
                case "max":
                    result.put("max", queryValues.stream().mapToDouble(d -> d).max().getAsDouble());
                    break;
                case "min":
                    result.put("min", queryValues.stream().mapToDouble(d -> d).min().getAsDouble());
                    break;
                case "average":
                    result.put("average", queryValues.stream().mapToDouble(d -> d).average().getAsDouble());
                    break;
                case "deviation":
                    StandardDeviation std = new StandardDeviation();
                    double[] unboxed = Stream.of(queryValues.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    result.put("deviation", std.evaluate(unboxed));
                    break;
                case "variance":
                    Variance vce = new Variance();
                    double[] unboxed2 = Stream.of(queryValues.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
                    result.put("variance", vce.evaluate(unboxed2));
                    break;
            }
        }
        return result;
    }

    private String getQuery(String metricName, Map<String, String> labelConfig) {
        StringBuilder queryBuilder = new StringBuilder(metricName);
        if (labelConfig != null) {
            queryBuilder.append("{");
            labelConfig.keySet().forEach(key -> {
                queryBuilder.append(key);
                queryBuilder.append("='");
                queryBuilder.append(labelConfig.get(key));
                queryBuilder.append("',");
            });
            queryBuilder.deleteCharAt(queryBuilder.length()-1);
            queryBuilder.append("}");
        }
        return queryBuilder.toString();
    }

    private String sendRequest(URI uri) {
        HttpGet request = new HttpGet(uri);
        String text;
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            text = EntityUtils.toString(entity);
        } catch (ClientProtocolException cpe) {
            System.err.println("ClientProtocolException while querying all Prometheus metrics from Prometheus server at " + prometheusUrl);
            return null;
        } catch (IOException ioe) {
            System.err.println("IOException while querying all Prometheus metrics from Prometheus server at " + prometheusUrl);
            return null;
        }
        return text;
    }
     */
}
