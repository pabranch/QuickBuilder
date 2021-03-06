package org.pitest.quickbuilder.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pitest.quickbuilder.builders.Builders.constant;
import static org.pitest.quickbuilder.builders.Builders.once;
import static org.pitest.quickbuilder.builders.Builders.repeat;

import org.junit.Test;
import org.pitest.quickbuilder.SequenceBuilder;
import org.pitest.quickbuilder.common.ComposedBuilder;

public class ComposedBuilderTest {

  @Test
  public void shouldCombineBuilders() {
    @SuppressWarnings("unchecked")
    SequenceBuilder<String> actual = ComposedBuilder.compose(once("foo"), once("bar"));
    assertThat(actual.buildAll()).containsExactly("foo", "bar");   
  }

  
  @Test
  public void shouldCombineSequences() {
    @SuppressWarnings("unchecked")
    SequenceBuilder<String> actual = ComposedBuilder.compose(repeat("foo",2), repeat("bar", 2));
    assertThat(actual.buildAll()).containsExactly("foo", "foo", "bar", "bar");   
  }
  
  @Test
  public void shouldBuildNothingWhenSuppliedWithNothing() {
    @SuppressWarnings("unchecked")
    SequenceBuilder<String> actual = ComposedBuilder.compose();
    assertThat(actual.buildAll()).isEmpty();  
  }
  
  @Test
  public void shouldLimitBuiltSequences() {
    @SuppressWarnings("unchecked")
    SequenceBuilder<String> actual = ComposedBuilder.compose(constant("1")).limit(3);
    assertThat(actual.buildAll()).hasSize(3);
  }
  
  @Test
  public void shouldBuildSequencesOfRequestedSize() {
    @SuppressWarnings("unchecked")
    SequenceBuilder<String> actual = ComposedBuilder.compose(constant("1"));
    assertThat(actual.build(3)).hasSize(3);
  }
  
  @Test
  public void shouldIterateOverValues() {
    @SuppressWarnings("unchecked")
    SequenceBuilder<String> actual = ComposedBuilder.compose(constant("1"));
    assertThat(actual.iterator().next()).isEqualTo("1");
  }
}
