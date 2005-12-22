/*
 * JBoss, Home of Professional Open Source  
 * 
 * Distributable under LGPL license. 
 * See terms of license at gnu.org.  
 */
package org.jboss.seam.contexts;

import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.event.PhaseId;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.jboss.logging.Logger;
import org.jboss.seam.Component;
import org.jboss.seam.Seam;
import org.jboss.seam.core.Init;
import org.jboss.seam.core.Manager;

/**
 * @author Gavin King
 * @author <a href="mailto:theute@jboss.org">Thomas Heute</a>
 * @version $Revision$
 */
public class Lifecycle
{

   private static final Logger log = Logger.getLogger( Lifecycle.class );

   public static void beginRequest(ExternalContext externalContext) {
      log.debug( ">>> Begin web request" );
      //eventContext.set( new WebRequestContext( request ) );
      Contexts.eventContext.set( new EventContext() );
      Contexts.sessionContext.set( new WebSessionContext( Session.getSession(externalContext, true) ) );
      Contexts.applicationContext.set( new FacesApplicationContext(externalContext) );
      Contexts.conversationContext.set(null); //in case endRequest() was never called
   }

   public static void beginRequest(ServletContext servletContext, HttpSession session) {
      log.debug( ">>> Begin web request" );
      //eventContext.set( new WebRequestContext( request ) );
      Contexts.eventContext.set( new EventContext() );
      Contexts.sessionContext.set( new WebSessionContext( Session.getSession(session) ) );
      Contexts.applicationContext.set( new WebApplicationContext(servletContext) );
      Contexts.conversationContext.set(null); //in case endRequest() was never called
   }

   public static void beginInitialization(ServletContext servletContext)
   {
      Context context = new WebApplicationContext(servletContext);
      Contexts.applicationContext.set(context);
   }

   public static void endInitialization()
   {
	  //instantiate @Startup components
      Context context = Contexts.getApplicationContext();
      for ( String name: context.getNames() ) {
    	  Object object = context.get(name);
    	  if ( object instanceof Component )
    	  {
	        Component component = (Component) object;
	        if ( component.isStartup() )
	        {
              startup(component);
	        }
    	  }
       }
      Contexts.applicationContext.set(null);
   }

   private static void startup(Component component)
   {
      if ( component.isStartup() )
      {
         for (String dependency: component.getDependencies() )
         {
            Component dependentComponent = Component.forName(dependency);
            if (dependentComponent!=null)
            {
               startup( dependentComponent );
            }
         }
      }
      Component.getInstance( component.getName(), true );
   }

   public static void endApplication(ServletContext servletContext)
   {
      log.debug("Undeploying, destroying application context");

      Context tempApplicationContext = new WebApplicationContext(servletContext);
      Contexts.applicationContext.set( tempApplicationContext );
      Contexts.destroy(tempApplicationContext);
      Contexts.applicationContext.set(null);
      Contexts.eventContext.set(null);
      Contexts.sessionContext.set(null);
      Contexts.conversationContext.set(null);
   }

   public static void endSession(ServletContext servletContext, Session session)
   {
      log.debug("End of session, destroying contexts");

      Context tempAppContext = new WebApplicationContext(servletContext);
      Contexts.applicationContext.set(tempAppContext);

      //this is used just as a place to stick the ConversationManager
      Context tempEventContext = new EventContext();
      Contexts.eventContext.set(tempEventContext);

      //this is used (a) for destroying session-scoped components
      //and is also used (b) by the ConversationManager
      Context tempSessionContext = new WebSessionContext( session );
      Contexts.sessionContext.set(tempSessionContext);

      Set<String> ids = Manager.instance().getSessionConversationIds();
      log.debug("destroying conversation contexts: " + ids);
      for (String conversationId: ids)
      {
         Contexts.destroy( new ConversationContext( session, conversationId) );
      }

      log.debug("destroying session context");
      Contexts.destroy(tempSessionContext);
      Contexts.sessionContext.set(null);

      Contexts.destroy(tempEventContext);
      Contexts.eventContext.set(null);

      Contexts.conversationContext.set(null);
      Contexts.applicationContext.set(null);
   }
   
   public static void flushConversation()
   {
      if ( Contexts.isConversationContextActive() )
      {
         if ( !Seam.isSessionInvalid() && Init.instance().isClientSideConversations() )
         {
            log.debug("flushing client-side conversation context");
            Contexts.getConversationContext().flush();
         }
      }   
   }

   public static void endRequest(ExternalContext externalContext) {

      log.debug("After render response, destroying contexts");

      try
      {
         flushAndDestroyContexts();

         if ( Seam.isSessionInvalid() )
         {
            Session.getSession(externalContext, true).invalidate(); //huh? we create a session just to invalidate it?
            //actual session context will be destroyed from the listener
         }
      }
      finally
      {
         clearThreadlocals();
      }

      log.debug( "<<< End web request" );
   }

   public static void endRequest() {

      log.debug("After request, destroying contexts");

      try
      {
         flushAndDestroyContexts();
      }
      finally
      {
         clearThreadlocals();
      }

      log.debug( "<<< End web request" );
   }

   public static void endRequest(HttpSession session) {

      log.debug("After request, destroying contexts");

      try
      {
         flushAndDestroyContexts();

         if ( Seam.isSessionInvalid() )
         {
            Session.getSession(session).invalidate(); //huh? we create a session just to invalidate it?
            //actual session context will be destroyed from the listener
         }
      }
      finally
      {
         clearThreadlocals();
      }

      log.debug( "<<< End web request" );
   }

   private static void clearThreadlocals() {
      Contexts.eventContext.set( null );
      Contexts.sessionContext.set( null );
      Contexts.conversationContext.set( null );
      Contexts.businessProcessContext.set( null );
      Contexts.applicationContext.set( null );
   }

   private static void flushAndDestroyContexts() {
      if ( Contexts.isBusinessProcessContextActive() )
      {
         log.debug("flushing busines process context");
         Contexts.getBusinessProcessContext().flush();
      }

      if ( Contexts.isEventContextActive() )
      {
         log.debug("destroying event context");
         Contexts.destroy( Contexts.getEventContext() );
      }

      if ( Contexts.isConversationContextActive() )
      {
         if ( !Manager.instance().isLongRunningConversation() )
         {
            log.debug("destroying conversation context");
            Contexts.destroy( Contexts.getConversationContext() );
         }
         if ( !Seam.isSessionInvalid() && !Init.instance().isClientSideConversations() )
         {
            log.debug("flushing server-side conversation context");
            Contexts.getConversationContext().flush();
         }
      }
   }

   public static void resumeConversation(ExternalContext externalContext, String id)
   {
      Init init = (Init) Component.getInstance(Init.class, false);
      Context conversationContext = init.isClientSideConversations() ?
            (Context) new ClientConversationContext() :
            (Context) new ConversationContext( Session.getSession(externalContext, true), id );
      Contexts.conversationContext.set( conversationContext );
   }

   public static void resumeConversation(HttpSession session, String id)
   {
      Context conversationContext = new ConversationContext( Session.getSession(session), id );
      Contexts.conversationContext.set( conversationContext );
   }

   public static void resumeBusinessProcess(Map state)
   {
      Contexts.businessProcessContext.set( new BusinessProcessContext(state) );
   }
   
   private static ThreadLocal<PhaseId> phaseId = new ThreadLocal<PhaseId>();
   
   public static PhaseId getPhaseId()
   {
      return phaseId.get();
   }
   
   public static void setPhaseId(PhaseId phase)
   {
      phaseId.set(phase);
   }
   
   private static ThreadLocal<ServletRequest> servletRequest = new ThreadLocal<ServletRequest>();

   public static ServletRequest getServletRequest() {
      return servletRequest.get();
   }

   public static void setServletRequest(ServletRequest servletRequest) {
      Lifecycle.servletRequest.set(servletRequest);
   }
   
}
