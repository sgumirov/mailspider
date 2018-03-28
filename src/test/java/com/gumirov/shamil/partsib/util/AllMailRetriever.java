package com.gumirov.shamil.partsib.util;

import com.icegreen.greenmail.server.AbstractServer;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllMailRetriever implements AutoCloseable {
  private AbstractServer server;
  private Store store;

  public AllMailRetriever(AbstractServer server) {
    this.server = server;
  }

  /**
   * Use account name as password.
   * @param account name
   * @return messages
   */
  public Message[] getMessages(String account) {
    return this.getMessages(account, account);
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
    ArrayList ret = new ArrayList();
    if((folder.getType() & 1) != 0) {
      if(!folder.isOpen()) {
        folder.open(1);
      }

      Message[] f = folder.getMessages();

      Collections.addAll(ret, f);
    }

    if((folder.getType() & 2) != 0) {
      Folder[] var8 = folder.list();
      Folder[] var4 = var8;
      int var5 = var8.length;

      for(int var6 = 0; var6 < var5; ++var6) {
        Folder aF = var4[var6];
        ret.addAll(this.getMessages(aF));
      }
    }

    return ret;
  }
}
