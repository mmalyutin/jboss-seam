package org.jboss.seam.core;

import static org.jboss.seam.InterceptionType.NEVER;

import java.io.Serializable;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Intercept;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;

@Name("actor")
@Scope(ScopeType.SESSION)
@Intercept(NEVER)
public class Actor implements Serializable {
   private String id;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public static Actor instance()
   {
      if ( !Contexts.isSessionContextActive() )
      {
         throw new IllegalStateException("No active session context");
      }
      return (Actor) Component.getInstance(Actor.class, true);
   }
}
