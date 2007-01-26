package org.jboss.seam.example.seamspace;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.Date;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.security.Restrict;

@Stateful
@Name("blog")
public class BlogAction implements BlogLocal
{    
   @RequestParameter
   private String name;   
   
   @RequestParameter
   private Integer blogId;
   
   @In(create=true)
   private EntityManager entityManager;
   
   @In(required = false)
   private Member selectedMember;   
   
   @In(required = false) @Out(required = false)
   private MemberBlog selectedBlog;
   
   @In(required = false) @Out(required = false, scope = CONVERSATION)
   private BlogComment comment;   
   
   @In(required = false)
   private Member authenticatedMember;
   
   /**
    * Used to read a single blog entry for a member
    */
   @Factory("selectedBlog") @Begin
   public void getBlog()
   {
      try
      {
         selectedBlog = (MemberBlog) entityManager.createQuery(
           "from MemberBlog b where b.blogId = :blogId and b.member.memberName = :memberName")
           .setParameter("blogId", blogId)
           .setParameter("memberName", name)
           .getSingleResult();
      }
      catch (NoResultException ex) { }
   }
   
   @Restrict 
   public void createComment()
   {      
      comment = new BlogComment();
      comment.setCommentor(authenticatedMember);
      
      if (selectedBlog == null && name != null && blogId != null)
         getBlog();         
      
      comment.setBlog(selectedBlog);
   }
   
   public void previewComment()
   {
      // don't really need to do anything here...
   }
   
   @End
   public void saveComment()
   {      
      comment.setCommentDate(new Date());
      entityManager.persist(comment);
      
      // Reload the blog entry
      entityManager.refresh(selectedBlog);
   }     
   
   @Begin
   public void createEntry()
   {
      selectedBlog = new MemberBlog();              
   }
   
   public void saveEntry()
   {
      selectedBlog.setMember(authenticatedMember);
      selectedBlog.setEntryDate(new Date());
      
      entityManager.persist(selectedBlog);
   }
   
   @Remove @Destroy
   public void destroy() { }     
}
