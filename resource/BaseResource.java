package com.sismics.reader.rest.resource;

import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.security.IPrincipal;
import com.sismics.security.UserPrincipal;
import com.sismics.util.filter.SecurityFilter;
import org.codehaus.jettison.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.security.Principal;
import java.util.Set;

/**
 * Base class of REST resources.
 * 
 * @author jtremeaux
 */

public class BaseResource {
    /**
     * Injects the HTTP request.
     */
    private HttpServletRequest request;
    private IPrincipal principal;
    public BaseResource(HttpServletRequest request){
        this.request = request;
    }
    // @Context
    // protected HttpServletRequest request;
    // public BaseResource() {}
    public void setRequest(HttpServletRequest request){
        this.request = request;
    }
    public HttpServletRequest getRequest(){
        return request;
    }
    /**
     * Application key.
     */
    @QueryParam("app_key")
    protected String appKey;
    
    /**
     * Principal of the authenticated user.
     */
    // protected IPrincipal principal;

    /**
     * This method is used to check if the user is authenticated.
     * 
     * @return True if the user is authenticated and not anonymous
     */
    public boolean authenticate() {
        Principal principal = (Principal) request.getAttribute(SecurityFilter.PRINCIPAL_ATTRIBUTE);
        if (principal != null && principal instanceof IPrincipal) {
            this.principal = (IPrincipal) principal;
            return !this.principal.isAnonymous();
        } else {
            return false;
        }
    }
    
    /**
     * Checks if the user has a base function. Throw an exception if the check fails.
     * 
     * @param baseFunction Base function to check
     */
    public void checkBaseFunction(BaseFunction baseFunction) throws JSONException {
        if (!hasBaseFunction(baseFunction)) {
            throw new ForbiddenClientException();
        }
    }
    
    /**
     * Checks if the user has a base function.
     * 
     * @param baseFunction Base function to check
     * @return True if the user has the base function
     */
    public boolean hasBaseFunction(BaseFunction baseFunction) throws JSONException {
        if (principal == null || !(principal instanceof UserPrincipal)) {
            return false;
        }
        Set<String> baseFunctionSet = ((UserPrincipal) principal).getBaseFunctionSet();
        return baseFunctionSet != null && baseFunctionSet.contains(baseFunction.name());
    }
    public IPrincipal getPrincipal() {
        return principal;
    }
}
