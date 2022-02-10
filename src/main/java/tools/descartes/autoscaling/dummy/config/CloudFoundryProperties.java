package tools.descartes.autoscaling.dummy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A property class containing settings with regard to CloudFoundry access and apps
 */
@Component
@ConfigurationProperties("cloudfoundry")
public class CloudFoundryProperties {
    /**
     * The cloudfoundry domain without "api." prefix
     */
    private String domain;

    /**
     * The organization to refer to in CloudFoundry
     */
    private String organization;

    /**
     * The space to refer to in CloudFoundry
     */
    private String space;

    /**
     * The user who has rights to scale applications
     */
    private String user;

    /**
     * The password for the user specified above
     */
    private String password;

    /**
     * A regex which matches all app names which should be included for scaling
     * If an app should be scaled it must match includeappregex and must not match excludeappregex. We do not use
     * Matcher.matches, we use Matcher.find
     */
    private String includeappregex;

    /**
     * A regex which matches all app names which should be excluded for scaling
     * If an app should be scaled it must match includeappregex and must not match excludeappregex. We do not use
     * Matcher.matches, we use Matcher.find
     */
    private String excludeappregex;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIncludeappregex() {
        return includeappregex;
    }

    public void setIncludeappregex(String includeappregex) {
        this.includeappregex = includeappregex;
    }

    public String getExcludeappregex() {
        return excludeappregex;
    }

    public void setExcludeappregex(String excludeappregex) {
        this.excludeappregex = excludeappregex;
    }

}
