package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Test fixture for repeating @Field with Collection return types.
 *
 * Layout (1-based offsets):
 *   positions  1-15 : 3 x String via List&lt;String&gt; (length=5, LEFT aligned)
 *   positions 16-25 : 2 x Integer via Collection&lt;Integer&gt; (length=5, RIGHT aligned, zero-padded)
 *   positions 26-37 : 3 x String via Set&lt;String&gt; (length=4, LEFT aligned) — loaded as LinkedHashSet
 */
@Record
public class RepeatingFieldCollectionRecord {

  private List<String> codes;
  private Collection<Integer> amounts;
  private Set<String> tags;

  @Field(offset = 1, length = 5, count = 3)
  public List<String> getCodes() {
    return codes;
  }

  public void setCodes(List<String> codes) {
    this.codes = codes;
  }

  @Field(offset = 16, length = 5, count = 2, align = Align.RIGHT, paddingChar = '0')
  public Collection<Integer> getAmounts() {
    return amounts;
  }

  public void setAmounts(Collection<Integer> amounts) {
    this.amounts = amounts;
  }

  @Field(offset = 26, length = 4, count = 3)
  public Set<String> getTags() {
    return tags;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }
}
