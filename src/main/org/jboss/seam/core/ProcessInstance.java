/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.core;

import static org.jboss.seam.InterceptionType.NEVER;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Intercept;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.contexts.Contexts;
import org.jbpm.db.JbpmSession;

/**
 * @author Gavin King
 * @version $Revision$
 */
@Scope(ScopeType.APPLICATION)
@Name("processInstance")
@Intercept(NEVER)
@Startup
public class ProcessInstance 
{
   
   @Unwrap
   public org.jbpm.graph.exe.ProcessInstance getProcessInstance()
   {
      if ( !Contexts.isConversationContextActive() ) return null;
      
      Long processId = Process.instance().getProcessId();
      if (processId!=null)
      {
         //TODO: should we cache this lookup?
         JbpmSession session = (JbpmSession) Component.getInstance( ManagedJbpmSession.class, true );
         return session.getGraphSession().loadProcessInstance(processId);
      }
      else
      {
         return null;
      }
   }
   
   public static org.jbpm.graph.exe.ProcessInstance instance()
   {
      if ( !Contexts.isApplicationContextActive() )
      {
         throw new IllegalStateException("No active application context");
      }
      return (org.jbpm.graph.exe.ProcessInstance) Component.getInstance(ProcessInstance.class, true);
   }
}
