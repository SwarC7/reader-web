package com.sismics.reader.rest.resource;

import com.sismics.reader.core.dao.jpa.FeedSubscriptionDao;
import com.sismics.reader.core.dao.jpa.UserArticleDao;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserArticleCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.assembler.ArticleAssembler;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.Context;

/**
 * All articles REST resources.
 * 
 * @author jtremeaux
 */
@Path("/all")
public class AllResource{
    private final BaseResource baseResource;
    public AllResource(@Context HttpServletRequest request) {
        baseResource = new BaseResource(request);
    }
    /**
     * Returns all articles.
     * 
     * @param unread Returns only unread articles
     * @param limit Page limit x
     * @param afterArticle Start the list after this user article
     * @return Response
     */


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
        @QueryParam("unread") boolean unread,
        @QueryParam("limit") Integer limit,
        @QueryParam("after_article") String afterArticle) throws JSONException {
        if (!baseResource.authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the articles
        UserArticleDao userArticleDao = new UserArticleDao();
        UserArticleCriteria userArticleCriteria = new UserArticleCriteria()
                .setUnread(unread)
                .setUserId(baseResource.getPrincipal().getId())
                .setSubscribed(true)
                .setVisible(true);
        if (afterArticle != null) {
            // Paginate after this user article
            UserArticleCriteria afterArticleCriteria = new UserArticleCriteria()
                    .setUserArticleId(afterArticle)
                    .setUserId(baseResource.getPrincipal().getId());
            List<UserArticleDto> userArticleDtoList = userArticleDao.findByCriteria(afterArticleCriteria);
            if (userArticleDtoList.isEmpty()) {
                throw new ClientException("ArticleNotFound", MessageFormat.format("Can't find user article {0}", afterArticle));
            }
            UserArticleDto userArticleDto = userArticleDtoList.iterator().next();

            userArticleCriteria.setArticlePublicationDateMax(new Date(userArticleDto.getArticlePublicationTimestamp()));
            userArticleCriteria.setArticleIdMax(userArticleDto.getArticleId());
        }

        PaginatedList<UserArticleDto> paginatedList = PaginatedLists.create(limit, null);
        userArticleDao.findByCriteria(paginatedList, userArticleCriteria, null, null);
        
        // Build the response
        JSONObject response = new JSONObject();

        List<JSONObject> articles = new ArrayList<JSONObject>();
        for (UserArticleDto userArticle : paginatedList.getResultList()) {
            articles.add(ArticleAssembler.asJson(userArticle));
        }
        response.put("articles", articles);

        return Response.ok().entity(response).build();
    }

    /**
     * Marks all articles as read.
     * 
     * @return Response
     */
    @POST
    @Path("/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read() throws JSONException {
        if (!baseResource.authenticate()) {
            throw new ForbiddenClientException();
        }
        
        // Marks all articles of this user as read
        UserArticleDao userArticleDao = new UserArticleDao();
        userArticleDao.markAsRead(new UserArticleCriteria()
                .setUserId(baseResource.getPrincipal().getId())
                .setSubscribed(true));

        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        for (FeedSubscriptionDto feedSubscrition : feedSubscriptionDao.findByCriteria(new FeedSubscriptionCriteria()
                .setUserId(baseResource.getPrincipal().getId()))) {
            feedSubscriptionDao.updateUnreadCount(feedSubscrition.getId(), 0);
        }

        // Always return ok
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

}
