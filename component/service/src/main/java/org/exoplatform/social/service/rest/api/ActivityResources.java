/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.service.rest.api;

import java.util.HashMap;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.StorageException;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.service.rest.SecurityManager;
import org.exoplatform.social.service.rest.Util;
import org.exoplatform.social.service.rest.api.models.Activity;
import org.exoplatform.social.service.rest.api.models.ActivityStream;
import org.exoplatform.social.service.rest.api.models.Comment;


/**
 * Activity Resources end point.
 * @author <a href="http://phuonglm.net">PhuongLM</a>
 * @author <a href="http://hoatle.net">hoatle (hoatlevan at gmail dot com)</a>
 * @since Jun 15, 2011
 */
@Path("api/social/" + VersionResources.LATEST_VERSION+ "/{portalContainerName}/")
public class ActivityResources implements ResourceContainer {

  private static final String[] SUPPORTED_FORMAT = new String[]{"json"};
  private ActivityManager activityManager;
  private IdentityManager identityManager;

  /**
   * Gets an activity by its id.
   *
   * @param uriInfo the uri request info
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity Id
   * @param format the expected returned format
   * @return a response object
   */
  @GET
  @Path("activity/{activityId}.{format}")
  public Response getActivityById(@Context UriInfo uriInfo,
                                  @PathParam("portalContainerName") String portalContainerName,
                                  @PathParam("activityId") String activityId,
                                  @PathParam("format") String format,
                                  @QueryParam("poster_identity") String showPosterIdentity,
                                  @QueryParam("number_of_comments") String numberOfComments,
                                  @QueryParam("activity_stream") String showActivityStream) {

    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT);

    if(activityId !=null && !activityId.equals("")){
      PortalContainer portalContainer = getPortalContainer(portalContainerName);
      activityManager = Util.getActivityManager();
      identityManager = Util.getIdentityManager();
      
      if(isAuthenticated()){
        try {
          //
          ActivityManager manager = Util.getActivityManager();
          ExoSocialActivity activity = manager.getActivity(activityId);
          if(!activity.isComment()){
            if(SecurityManager.canAccessActivity(portalContainer,ConversationState.getCurrent().getIdentity().getUserId(),activity)){
              //
              Activity model = new Activity(activity);
        
              //
              if (isPassed(showPosterIdentity)) {
                model.setPosterIdentity(new org.exoplatform.social.service.rest.api.models.Identity(
                    identityManager.getIdentity(activity.getUserId(), false)));
              }
        
              //
              if (isPassed(showActivityStream)) {
                model.setActivityStream(new ActivityStream(activity.getActivityStream()));
              }
        
              //
              if (numberOfComments != null) {
        
                int commentNumber = activity.getReplyToId().length;
                int number = Integer.parseInt(numberOfComments);
        
                if (number > 100) {
                  number = 100;
                }
                
                if (number > commentNumber) {
                  number = commentNumber;
                }
                ListAccess<ExoSocialActivity> comments =  activityManager.getCommentsWithListAccess(activity);
                ExoSocialActivity[] commentsLimited =  comments.load(0, number);
                Comment[] commentWrapers = new Comment[number];
                for(int i = 0; i < number; i++){
                  commentWrapers[i] = new Comment(commentsLimited[i]);      
                }
                model.setComments(commentWrapers);
                
                model.setComments(commentWrapers);
              }
              model.setTotalNumberOfComments(activity.getReplyToId().length);
              
              if(isLikedByIdentity(authenticatedUserIdentity().getId(),activity)){
                model.setLiked(true);
              } else {
                model.setLiked(false);
              }
              
              return Util.getResponse(model, uriInfo, mediaType, Response.Status.OK);
            } else {
              throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
          } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
          }
        }
        catch (Exception e) {
          throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
      } else {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    
  }


  /**
   * Creates a new activity.
   *
   * @param uriInfo the uri request info
   * @param portalContainerName the associated portal container name
   * @param format the expected returned format
   * @param identityIdStream the optional identity stream to post this new activity to
   * @param newActivity a new activity instance
   * @return a response object
   */
  @POST
  @Path("activity.{format}")
  public Response createNewActivity(@Context UriInfo uriInfo,
                                    @PathParam("portalContainerName") String portalContainerName,
                                    @PathParam("format") String format,
                                    @QueryParam("identity_id") String identityIdStream,
                                    Activity newActivity) {

    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT);
    
    if(newActivity !=null && newActivity.getTitle()!=null && !newActivity.getTitle().equals("")){
        try {
          ActivityManager activityManager = Util.getActivityManager();
          IdentityManager identityManager =  Util.getIdentityManager();
          PortalContainer portalContainer = getPortalContainer(portalContainerName);
          

          Identity authenticatedIdentity = authenticatedUserIdentity();
          Identity postToIdentity;

          if(identityIdStream != null && !identityIdStream.equals("")){
            postToIdentity = identityManager.getIdentity(identityIdStream, false);
            if(postToIdentity == null){
              throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
          } else {
            postToIdentity = authenticatedIdentity;
          }
          
          if(authenticatedIdentity != null && SecurityManager.canPostActivity(portalContainer, authenticatedIdentity, postToIdentity)){
            ExoSocialActivity activity = new ExoSocialActivityImpl();          

            activity.setId(newActivity.getId());
            activity.setTitle(newActivity.getTitle());
            activity.setType(newActivity.getType());
            activity.setPriority(newActivity.getPriority());
            activity.setTitleId(newActivity.getTitleId());
            activity.setTemplateParams(newActivity.getTemplateParams());
            
            newActivity.setIdentityId(authenticatedIdentity.getId());
            activityManager.saveActivityNoReturn(postToIdentity, activity);
            
            //
            Activity model = new Activity(activity);
            model.setIdentityId(postToIdentity.getId());
      
            return Util.getResponse(model, uriInfo, mediaType, Response.Status.OK);
          } else {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
          }
        }
        catch (StorageException e) {
          throw new WebApplicationException( Response.Status.INTERNAL_SERVER_ERROR);
        }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  /**
   * Deletes an existing activity by DELETE method from a specified activity id. Just returns the deleted activity
   * object.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @DELETE
  @Path("activity/{activityId}.{format}")
  public Response deleteExistingActivityById(@Context UriInfo uriInfo,
                                            @PathParam("portalContainerName") String portalContainerName,
                                            @PathParam("activityId") String activityId,
                                            @PathParam("format") String format) {
    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT); 
    if(activityId!=null && portalContainerName!=null ){      
      activityManager = Util.getActivityManager();
      identityManager = Util.getIdentityManager();
      PortalContainer portalContainer = getPortalContainer(portalContainerName);
      ExoSocialActivity existingActivity = activityManager.getActivity(activityId);
      Identity authenticatedIdentity = authenticatedUserIdentity();
      
      if(authenticatedIdentity != null){
        if(SecurityManager.canDeleteActivity(portalContainer, authenticatedIdentity, existingActivity)){
          activityManager.deleteActivity(existingActivity);
          Activity model = new Activity(existingActivity);
          return Util.getResponse(model, uriInfo, mediaType, Response.Status.OK);
        } else {
          throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
      } else {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  /**
   * Deletes an existing activity by POST method from a specified activity id. Just returns the deleted activity
   * object. Deletes by DELETE method is recommended. This API should be used only when DELETE method is not supported
   * by the client.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @POST
  @Path("activity/destroy/{activityId}.{format}")
  public Response postToDeleteActivityById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format) {
    return deleteExistingActivityById(uriInfo, portalContainerName, activityId, format);
  }

  /**
   * Get Comment from existing activity by GET method from a specified activity id. Just returns the Comment List and total number of Comment
   * in activity.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @GET
  @Path("activity/{activityId}/comments.{format}")
  public Response getCommentsByActivityById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format) {
    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT);

    if(activityId !=null && !activityId.equals("")){
      PortalContainer portalContainer = getPortalContainer(portalContainerName);
      activityManager = Util.getActivityManager();
      identityManager = Util.getIdentityManager();
      Identity authenticatedUserIdentity = authenticatedUserIdentity();
      ExoSocialActivity activity = activityManager.getActivity(activityId);
      if(authenticatedUserIdentity != null){
        if(SecurityManager.canAccessActivity(portalContainer, authenticatedUserIdentity, activity)){
          int total;
          Comment[] commentWrapers = null;
          try {
            ListAccess<ExoSocialActivity> comments =  activityManager.getCommentsWithListAccess(activity);
            total = comments.getSize();
            ExoSocialActivity[] commentsLimited =  comments.load(0, total);
            commentWrapers = new Comment[total];
            for(int i = 0; i < total; i++){
              commentWrapers[i] = new Comment(commentsLimited[i]);      
            }
          } catch (Exception e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
          }
          
          HashMap resultJson = new HashMap();
          resultJson.put("total", commentWrapers.length);
          resultJson.put("comments", commentWrapers);
          return Util.getResponse(resultJson, uriInfo, mediaType, Response.Status.OK);
        } else {
          throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
      } else {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }
  
  /**
   * Comment an existing activity by POST method from a specified activity id. Just returns the created comment.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @POST
  @Path("activity/{activityId}/comment.{format}")
  public Response createCommentActivityById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format,
                                           Comment comment) {
    
    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT);

    if(comment !=null && comment.getText() != null && !comment.getText().equals("")){
      PortalContainer portalContainer = getPortalContainer(portalContainerName);
      activityManager = Util.getActivityManager();
      identityManager = Util.getIdentityManager();
      Identity authenticatedUserIdentity = authenticatedUserIdentity();
      ExoSocialActivity activity = activityManager.getActivity(activityId);
      if(authenticatedUserIdentity != null){
        if(SecurityManager.canCommentToActivity(portalContainer, authenticatedUserIdentity, activity)){
          
          ExoSocialActivity commentActivity = new ExoSocialActivityImpl();
          commentActivity.setTitle(comment.getText());
          commentActivity.setUserId(authenticatedUserIdentity.getId());
          
          activityManager.saveComment(activity,commentActivity);
          
          comment = new Comment(commentActivity);
          
          return Util.getResponse(comment, uriInfo, mediaType, Response.Status.OK);
        } else {
          throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
      } else {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }
  
  /**
   * Comment an existing activity by POST method from a specified activity id. Just returns the created comment.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @DELETE
  @Path("activity/{activityId}/comment/{commentId}.{format}")
  public Response deleteCommentById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format,
                                           @PathParam("commentId") String commentId) {
    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT);

    if(commentId !=null && !commentId.equals("")){
      PortalContainer portalContainer = getPortalContainer(portalContainerName);
      activityManager = Util.getActivityManager();
      identityManager = Util.getIdentityManager();
      Identity authenticatedUserIdentity = authenticatedUserIdentity();
      ExoSocialActivity commentActivity = activityManager.getActivity(commentId);
      ExoSocialActivity activity = activityManager.getActivity(commentId);
      if(commentActivity.isComment()){
        if(authenticatedUserIdentity != null){
          if(SecurityManager.canDeleteActivity(portalContainer, authenticatedUserIdentity, commentActivity)){
            activityManager.deleteComment(activity, commentActivity);
            Comment resultComment = new Comment(commentActivity);
            return Util.getResponse(resultComment, uriInfo, mediaType, Response.Status.OK);
          } else {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
          }
        } else {
          throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
      } else {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
      }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }
  
  /**
   * Delete a Comment in activity spectify by commentId and activityId using DELETE.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @POST
  @Path("activity/{activityId}/comment/destroy/{commentId}.{format}")
  public Response postDeleteCommentById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format,
                                           @PathParam("commentId") String commentId) {
    return deleteCommentById(uriInfo, portalContainerName, activityId, format, commentId);
  }
  
  
  /**
   * Like an existing activity by POST method from a specified activity id. Just returns {"liked": "true"} if success.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @POST
  @Path("activity/{activityId}/like.{format}")
  public Response createLikeActivityById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format) {
    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT);

    if(activityId !=null && !activityId.equals("")){
      PortalContainer portalContainer = getPortalContainer(portalContainerName);
      activityManager = Util.getActivityManager();
      identityManager = Util.getIdentityManager();
      Identity authenticatedUserIdentity = authenticatedUserIdentity();
      ExoSocialActivity activity = activityManager.getActivity(activityId);
      if(authenticatedUserIdentity != null){
        if(SecurityManager.canCommentToActivity(portalContainer, authenticatedUserIdentity, activity)){        
          
          activityManager.saveLike(activity, authenticatedUserIdentity());
          
          HashMap resultJson = new HashMap();
          resultJson.put("like", true);
          
          return Util.getResponse(resultJson, uriInfo, mediaType, Response.Status.OK);
        } else {
          throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
      } else {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  /**
   * Like an existing activity by POST method from a specified activity id. Just returns {"liked": "true"} if success.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @DELETE
  @Path("activity/{activityId}/like.{format}")
  public Response deleteLikeActivityById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format) {
    MediaType mediaType = Util.getMediaType(format, SUPPORTED_FORMAT);

    if(activityId !=null && !activityId.equals("")){
      PortalContainer portalContainer = getPortalContainer(portalContainerName);
      activityManager = Util.getActivityManager();
      identityManager = Util.getIdentityManager();
      Identity authenticatedUserIdentity = authenticatedUserIdentity();
      ExoSocialActivity activity = activityManager.getActivity(activityId);
      if(authenticatedUserIdentity != null){
        if(SecurityManager.canCommentToActivity(portalContainer, authenticatedUserIdentity, activity)){        
          
          activityManager.deleteLike(activity, authenticatedUserIdentity());
          
          HashMap resultJson = new HashMap();
          resultJson.put("like", false);
          
          return Util.getResponse(resultJson, uriInfo, mediaType, Response.Status.OK);
        } else {
          throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
      } else {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }
    } else {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }
  
  /**
   * Like an existing activity by POST method from a specified activity id. Just returns {"liked": "true"} if success.
   *
   * @param uriInfo the uri request uri
   * @param portalContainerName the associated portal container name
   * @param activityId the specified activity id
   * @param format the expected returned format
   * @return a response object
   */
  @POST
  @Path("activity/{activityId}/like/destroy.{format}")
  public Response postDeleteLikeActivityById(@Context UriInfo uriInfo,
                                           @PathParam("portalContainerName") String portalContainerName,
                                           @PathParam("activityId") String activityId,
                                           @PathParam("format") String format) {
    return deleteLikeActivityById(uriInfo, portalContainerName, activityId, format);
  }
  
  private boolean isLikedByIdentity(String identityID, ExoSocialActivity activity){
    String[] likedIdentityIds = activity.getLikeIdentityIds();
    if(activity.getLikeIdentityIds()!=null && likedIdentityIds.length > 0 ){
      for (int i = 0; i < likedIdentityIds.length; i++) {
        if (identityID.equals(likedIdentityIds[i])){
          return true;
        }
      }
    }
    return false;
  }
  
  private boolean isAuthenticated() {
    return ConversationState.getCurrent()!=null && ConversationState.getCurrent().getIdentity() != null &&
              ConversationState.getCurrent().getIdentity().getUserId() != null;
  }
  
  private Identity authenticatedUserIdentity() {
    if(ConversationState.getCurrent()!=null && ConversationState.getCurrent().getIdentity() != null &&
              ConversationState.getCurrent().getIdentity().getUserId() != null){
      IdentityManager identityManager =  Util.getIdentityManager();
      String authenticatedUserRemoteID = ConversationState.getCurrent().getIdentity().getUserId(); 
      return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUserRemoteID, false);
    } else {
      return null;
    }
  }
  
  private boolean isPassed(String value) {
    return value != null && ("true".equals(value) || "t".equals(value) || "1".equals(value));
  }

  private PortalContainer getPortalContainer(String name) {
    return (PortalContainer) ExoContainerContext.getContainerByName(name);
  }
}
