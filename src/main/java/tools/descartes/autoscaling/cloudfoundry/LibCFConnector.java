package tools.descartes.autoscaling.cloudfoundry;

import org.cloudfoundry.client.v3.applications.*;
import org.cloudfoundry.client.v3.processes.GetProcessStatisticsRequest;
import org.cloudfoundry.client.v3.processes.GetProcessStatisticsResponse;
import org.cloudfoundry.client.v3.processes.ProcessStatisticsResource;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.descartes.autoscaling.config.CloudFoundryProperties;
import tools.descartes.autoscaling.scaling.ScalingRequest;
import tools.descartes.autoscaling.scaling.ScalingResponse;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An implementation of CFConnector which works with the official CloudFoundry Lib
 */
public class LibCFConnector implements CFConnector {

    private static final Logger logger = LoggerFactory.getLogger(LibCFConnector.class);

    private final Pattern includeRegex;
    private final Pattern excludeRegex;
    private final ReactorCloudFoundryClient cloudFoundryClient;
    private final DefaultCloudFoundryOperations cloudFoundryOperations;
    private final Map<String, String> eurekaToCloudfoundryNames;

    public LibCFConnector(CloudFoundryProperties cfProperties, Map<String, String> eurekaToCloudfoundryNames
    ) {
        this.includeRegex = Pattern.compile(cfProperties.getIncludeappregex());
        this.excludeRegex = Pattern.compile(cfProperties.getExcludeappregex());
        this.eurekaToCloudfoundryNames = eurekaToCloudfoundryNames;
        ConnectionContext context = DefaultConnectionContext.builder()
                .skipSslValidation(true)
                .apiHost("api." + cfProperties.getDomain())
                .build();
        PasswordGrantTokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
                .password(cfProperties.getPassword())
                .username(cfProperties.getUser())
                .build();
        cloudFoundryClient = ReactorCloudFoundryClient.builder()
                .connectionContext(context)
                .tokenProvider(tokenProvider)
                .build();
        cloudFoundryOperations = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cloudFoundryClient)
                .organization(cfProperties.getOrganization())
                .space(cfProperties.getSpace())
                .build();
    }

    @Override
    public Map<String, Integer> getAppInstances() {
        return getApps()
                .collectMap(ApplicationSummary::getName, ApplicationSummary::getInstances)
                .block();
    }

    @Override
    public ScalingResponse scaleApp(ScalingRequest sr) {
        String appName = cloudfoundryAppName(sr.getApp());
        logger.debug("scaling app= {}, target instances={}", appName, sr.getInstances());
        ScaleApplicationRequest sar = ScaleApplicationRequest.builder()
                .name(appName)
                .instances(sr.getInstances())
                .build();
        try {
            cloudFoundryOperations.applications().scale(sar).block();
            return new ScalingResponse(true, null);
        } catch (Exception e) {
            logger.error("Error while executing scaling: " + e.toString());
        }
        return new ScalingResponse(false, "Error during scaling");
    }

    private Flux<ApplicationSummary> getApps() {
        return cloudFoundryOperations.applications()
                .list()
                .filter(as -> includeRegex.matcher(as.getName()).find() && !excludeRegex.matcher(as.getName()).find());
    }

    private List<ProcessStatisticsResource> getResourcesForApp(ApplicationSummary app) {
        return Mono.just(app).map(ApplicationSummary::getId)
                .flatMap(this::getProcessId)
                .flatMap(processId -> cloudFoundryClient.processes()
                        .getStatistics(GetProcessStatisticsRequest.builder().processId(processId).build()))
                .flatMapIterable(GetProcessStatisticsResponse::getResources)
                .filter(r -> r.getType().equals("web"))
                .collectList()
                .block();
    }

    private Mono<String> getProcessId(String appId) {
        return cloudFoundryClient.applicationsV3()
                .getProcess(GetApplicationProcessRequest.builder().applicationId(appId).type("web").build())
                .map(GetApplicationProcessResponse::getId);
    }

    private String cloudfoundryAppName(String eurekaAppName) {
        return eurekaToCloudfoundryNames.getOrDefault(eurekaAppName, eurekaAppName);
    }
}
