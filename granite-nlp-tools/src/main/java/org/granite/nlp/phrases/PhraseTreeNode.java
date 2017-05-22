package org.granite.nlp.phrases;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.UUID;

public class PhraseTreeNode {

  private String key;
  private String unstemmedKey;
  private final UUID nodeId = UUID.randomUUID();
  private HashMap<String, PhraseTreeNode> childNodes = new HashMap<>();
  private HashMap<String, PhraseTreeNode> parentNodes = new HashMap<>();

  public PhraseTreeNode(String key) {
    this(key, key);
  }

  public PhraseTreeNode(String key, String unstemmedKey) {
    this.key = checkNotNull(key, "key");
    this.unstemmedKey = checkNotNull(unstemmedKey, "unstemmedKey");
  }

  public String getKey() {
    return key;
  }

  public UUID getNodeId() {
    return nodeId;
  }

  public String getUnstemmedKey() {
    return unstemmedKey;
  }

  public HashMap<String, PhraseTreeNode> getChildNodes() {
    return childNodes;
  }

  public HashMap<String, PhraseTreeNode> getParentNodes() {
    return parentNodes;
  }

}
