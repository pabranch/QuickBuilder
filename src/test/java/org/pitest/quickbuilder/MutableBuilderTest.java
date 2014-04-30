package org.pitest.quickbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.example.beans.MutableByExtension;
import com.example.beans.MutableFruitBuilder;

public class MutableBuilderTest {

  @Test
  public void shouldImplementButMethodThatReturnsCopyOfTheBuilder() {
    final MutableFruitBuilder builder = QB.builder(MutableFruitBuilder.class);
    assertThat(builder.but()).isNotSameAs(builder);
  }

  @Test
  public void shouldUpdateStateOfBuilderObjectWhenWithMethodCalled() {
    final MutableFruitBuilder builder = QB.builder(MutableFruitBuilder.class);
    final MutableFruitBuilder original = builder.withName("original");
    assertThat(builder.withName("foo").build().getName()).isEqualTo("foo");
    assertThat(original.build().getName()).isEqualTo("foo");
  }

  @Test
  public void shouldInheritWithMethodsFromExtendedInterface() {
    final MutableByExtension builder = QB.builder(MutableByExtension.class);
    assertThat(builder.withName("foo").build().getName()).isEqualTo("foo");
  }

  @Test
  public void shouldInheritUnderscoreMethodsFromExtendedInterface() {
    final MutableByExtension builder = QB.builder(MutableByExtension.class);
    assertThat(builder.withFoo("foo")._Foo()).isEqualTo("foo");
  }

}
