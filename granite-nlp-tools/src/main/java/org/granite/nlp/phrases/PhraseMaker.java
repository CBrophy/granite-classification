package org.granite.nlp.phrases;

import com.google.common.collect.ImmutableList;

public interface PhraseMaker {

  ImmutableList<String> rawTextToPhrase(final String rawText);

  String rawTextToCorrectedPhrase(final String rawText);
}
