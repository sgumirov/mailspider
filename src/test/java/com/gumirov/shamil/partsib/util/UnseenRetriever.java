package com.gumirov.shamil.partsib.util;

import com.icegreen.greenmail.server.AbstractServer;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Retrieves unseen messages from Greenmail. Helper for automated tests.
 */
public class UnseenRetriever implements AutoCloseable {
  private AbstractServer server;
  private Store store;

  public UnseenRetriever(AbstractServer server) {
    this.server = server;
  }

  /**
   * Use account name same as password.
   * @param accountAndPassword name
   * @return messages
   */
  public Message[] getMessages(String accountAndPassword) {
    return this.getMessages(accountAndPassword, accountAndPassword);
  }

  public Message[] getMessages(String account, String password) {
    try {
      this.store = this.server.createStore();
      this.store.connect(this.server.getBindTo(), this.server.getPort(), account, password);
      Folder e = this.store.getFolder("INBOX");
      List messages = this.getMessages(e);
      FetchProfile fp = new FetchProfile();
      fp.add(UIDFolder.FetchProfileItem.UID);
      e.fetch(e.getMessages(), fp);
      return (Message[])messages.toArray(new Message[messages.size()]);
    } catch (MessagingException var6) {
      throw new RuntimeException(var6);
    }
  }

  /** @deprecated */
  @Deprecated
  public void logout() {
    this.close();
  }

  public void close() {
    try {
      this.store.close();
    } catch (MessagingException var2) {
      throw new RuntimeException(var2);
    }
  }

  private List<Message> getMessages(Folder folder) throws MessagingException {
    ArrayList<Message> ret = new ArrayList<>();
    if((folder.getType() & 1) != 0) {
      if(!folder.isOpen()) {
        folder.open(1);
      }

      //Message[] f = folder.getMessages();
      Message[] f = folder.search(
          new FlagTerm(new Flags(Flags.Flag.SEEN), false));
      
      Collections.addAll(ret, f);
    }

    if ((folder.getType() & 2) != 0) {
      Folder[] files = folder.list();
      int l = files.length;
      for (Folder f : files) {
        ret.addAll(this.getMessages(f));
      }
    }

    return ret;
  }
}
