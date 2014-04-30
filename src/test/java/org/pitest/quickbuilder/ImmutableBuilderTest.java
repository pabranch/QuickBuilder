package org.pitest.quickbuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.pitest.quickbuilder.sequence.ElementSequence;
import org.pitest.quickbuilder.sequence.NonBuilder;

import com.example.beans.ArrayBean;
import com.example.beans.ArrayBeanBuilder;
import com.example.beans.ByteArrayBean;
import com.example.beans.ByteArrayBeanBuilder;
import com.example.beans.ChildBean;
import com.example.beans.ChildBeanBuilder;
import com.example.beans.CompositeBean;
import com.example.beans.CompositeBeanBuilder;
import com.example.beans.FruitBean;
import com.example.beans.FruitBuilder;
import com.example.beans.GenericPropertiesBean;
import com.example.beans.GenericPropertiesBeanBuilder;
import com.example.beans.PropertyOverridenByUnderscore;
import com.example.beans.StatelessBeanBuilder;
import com.example.beans.StringBean;
import com.example.beans.StringBeanBuilder;
import com.example.beans.generics.BuilderDeclaringBaseBuilderProperty;
import com.example.beans.generics.BuilderDeclaringBoundedWildcardProperty;
import com.example.beans.misuse.BuilderDeclaringNonExistingProperty;
import com.example.beans.misuse.BuilderWithParameterisedUnderscoreMethod;
import com.example.beans.misuse.BuilderWithPropertyReturningWrongType;
import com.example.beans.misuse.BuilderWithPropertyWithTooManyParameters;
import com.example.beans.misuse.BuilderWithTypeMismatchInUnderscoreMethod;
import com.example.beans.primitives.BooleanBean;
import com.example.beans.primitives.BooleanBeanBuilder;
import com.example.beans.primitives.ByteBean;
import com.example.beans.primitives.ByteBeanBuilder;
import com.example.beans.primitives.CharBean;
import com.example.beans.primitives.CharBeanBuilder;
import com.example.beans.primitives.DoubleBean;
import com.example.beans.primitives.DoubleBeanBuilder;
import com.example.beans.primitives.FloatBean;
import com.example.beans.primitives.FloatBeanBuilder;
import com.example.beans.primitives.IntBean;
import com.example.beans.primitives.IntBeanBuilder;
import com.example.beans.primitives.LongBean;
import com.example.beans.primitives.LongBeanBuilder;
import com.example.beans.primitives.PrimitiveBeanBuilder;
import com.example.beans.primitives.ShortBean;
import com.example.beans.primitives.ShortBeanBuilder;
import com.example.example.Apple;
import com.example.example.Fruit;
import com.example.example.FruitBuilders;
import com.example.immutable.IntegerValue;
import com.example.immutable.IntegerValueBuilder;
import com.example.immutable.MixedValue;
import com.example.immutable.MixedValueBuilder;
import com.example.immutable.MixedValueGenerator;

public class ImmutableBuilderTest {
  
  @Test
  public void shouldCreateABuilder() {
    final StatelessBeanBuilder testee = QB.builder(StatelessBeanBuilder.class);
    assertThat(testee).isNotNull();
  }

  @Test
  public void shouldBuildBeans() {
    final StatelessBeanBuilder builder = QB.builder(StatelessBeanBuilder.class);
    assertThat(builder.build()).isNotNull();
  }

  @Test
  public void shouldImplementWithMethodsThatReturnCopyOfTheBuilder() {
    final FruitBuilder builder = QB.builder(FruitBuilder.class);
    assertThat(builder.withName("foo")).isNotSameAs(builder);
  }

  @Test
  public void shouldNotChangeStateInBuilderWhenWithMethodCalled() {
    final FruitBuilder builder = QB.builder(FruitBuilder.class);
    final FruitBuilder original = builder.withName("original");
    assertThat(builder.withName("foo").build().getName()).isEqualTo("foo");
    assertThat(original.build().getName()).isEqualTo("original");
  }

  @Test
  public void shouldSupportAnyLowerCasePrefixForBuilderMethods() {
    final FruitBuilder builder = QB.builder(FruitBuilder.class);
    assertThat(builder.andColour("foo")).isNotSameAs(builder);
  }

  @Test
  public void shouldSetStateOnBeansForStringProperty() {
    final StringBeanBuilder builder = QB.builder(StringBeanBuilder.class);
    assertThat(builder.withName("foo").build().getName()).isEqualTo("foo");
  }

  @Test
  public void shouldSetStateOnBeansForMultipleStringProperties() {
    final FruitBuilder builder = QB.builder(FruitBuilder.class);
    final FruitBean fruit = builder.withName("foo").andColour("foo").build();
    assertThat(fruit.getName()).isEqualTo("foo");
    assertThat(fruit.getColour()).isEqualTo("foo");
  }

  @Test
  public void shouldSetStateOnBeansForIntProperty() {
    final IntBeanBuilder builder = QB.builder(IntBeanBuilder.class);
    final IntBean b = builder.withI(42).build();
    assertThat(b.getI()).isEqualTo(42);
  }

  @Test
  public void shouldSetStateOnBeansForLongProperty() {
    final LongBeanBuilder builder = QB.builder(LongBeanBuilder.class);
    final LongBean b = builder.withL(42l).build();
    assertThat(b.getL()).isEqualTo(42);
  }

  @Test
  public void shouldSetStateOnBeansForFloatProperty() {
    final FloatBeanBuilder builder = QB.builder(FloatBeanBuilder.class);
    final FloatBean b = builder.withF(42f).build();
    assertThat(b.getF()).isEqualTo(42f);
  }

  @Test
  public void shouldSetStateOnBeansForDoubleProperty() {
    final DoubleBeanBuilder builder = QB.builder(DoubleBeanBuilder.class);
    final DoubleBean b = builder.withD(42d).build();
    assertThat(b.getD()).isEqualTo(42d);
  }

  @Test
  public void shouldSetStateOnBeansForBooleanProperty() {
    final BooleanBeanBuilder builder = QB.builder(BooleanBeanBuilder.class);
    final BooleanBean b = builder.withB(true).build();
    assertThat(b.isB()).isEqualTo(true);
  }

  @Test
  public void shouldSetStateOnBeansForShortProperty() {
    final ShortBeanBuilder builder = QB.builder(ShortBeanBuilder.class);
    final ShortBean b = builder.withS((short) 3).build();
    assertThat(b.getS()).isEqualTo((short) 3);
  }

  @Test
  public void shouldSetStateOnBeansForCharProperty() {
    final CharBeanBuilder builder = QB.builder(CharBeanBuilder.class);
    final CharBean b = builder.withC('a').build();
    assertThat(b.getC()).isEqualTo('a');
  }

  @Test
  public void shouldSetStateOnBeansForByteProperty() {
    final ByteBeanBuilder builder = QB.builder(ByteBeanBuilder.class);
    final ByteBean b = builder.withBy((byte) 1).build();
    assertThat(b.getBy()).isEqualTo((byte) 1);
  }

  @Test
  public void shouldSetStateOnBeansForByteArrayProperty() {
    final ByteArrayBeanBuilder builder = QB.builder(ByteArrayBeanBuilder.class);
    final byte[] bs = new byte[] {};
    final ByteArrayBean b = builder.withBytes(bs).build();
    assertThat(b.getBytes()).isSameAs(bs);
  }

  @Test
  public void shouldSetStateOnBeansForStringArrayProperty() {
    final ArrayBeanBuilder builder = QB.builder(ArrayBeanBuilder.class);
    final String[] s = new String[] {};
    final ArrayBean b = builder.withStrings(s).build();
    assertThat(b.getStrings()).isSameAs(s);
  }

  @Test
  public void shouldSetStateOnBeansForDoubleArrayProperty() {
    final ArrayBeanBuilder builder = QB.builder(ArrayBeanBuilder.class);
    final double[] d = new double[] {};
    final ArrayBean b = builder.withDoubles(d).build();
    assertThat(b.getDoubles()).isSameAs(d);
  }

  @Test
  public void shouldSetStateOnBeansForBooleanArrayProperty() {
    final ArrayBeanBuilder builder = QB.builder(ArrayBeanBuilder.class);
    final boolean[] d = new boolean[] {};
    final ArrayBean b = builder.withBools(d).build();
    assertThat(b.getBools()).isSameAs(d);
  }

  @Test
  public void shouldSetStateOnBeansForMultiDimensionalArrayProperty() {
    final ArrayBeanBuilder builder = QB.builder(ArrayBeanBuilder.class);
    final boolean[][][][][] multi = new boolean[][][][][] {};
    final ArrayBean b = builder.withMulti(multi).build();
    assertThat(b.getMulti()).isSameAs(multi);
  }

  @Test
  public void shouldUseCorrectMethodsInHierarchy() {
    final ChildBeanBuilder builder = QB.builder(ChildBeanBuilder.class);
    ChildBean bean = builder.withFoo("foo").withBar("bar").build();
    assertThat(bean.getBar()).isEqualTo("bar");
    assertThat(bean.getFoo()).isEqualTo("modifiedbychild_foo");

  }

  @Test
  public void shouldSetStateOnBeansForGenericListProperty() {
    final GenericPropertiesBeanBuilder builder = QB
        .builder(GenericPropertiesBeanBuilder.class);
    final List<String> expected = Collections.emptyList();
    final GenericPropertiesBean b = builder.withS(expected).build();
    assertThat(b.getS()).isSameAs(expected);
  }

  @Test
  public void shouldSetStateOnBeansForBoundedWildCardGenericListProperty() {
    final GenericPropertiesBeanBuilder builder = QB
        .builder(GenericPropertiesBeanBuilder.class);
    final List<? extends Number> expected = Collections.emptyList();
    final GenericPropertiesBean b = builder.withN(expected).build();
    assertThat(b.getN()).isSameAs(expected);
  }

  @Test
  public void shouldImplementUnderscoreAccessorsForInteger() {
    final IntegerValueBuilder builder = QB.builder(IntegerValueBuilder.class,
        intGenerator());
    assertThat(builder.withI(42)._I()).isEqualTo(42);
  }

  @Test
  public void shouldContructImmutableValueTypesWithInteger() {
    final IntegerValueBuilder builder = QB.builder(IntegerValueBuilder.class,
        intGenerator());
    assertThat(builder.withI(42).build().i()).isEqualTo(42);
  }
  
  public interface MaybeStringBeanBuilder extends Builder<StringBean> {
    
    MaybeStringBeanBuilder withName(String name);
    
    Maybe<String> __Name();
    
  }
  
  @Test
  public void shouldImplementUnderscoreAccessorsForMaybes() {
    MaybeStringBeanBuilder builder = QB.builder(MaybeStringBeanBuilder.class);
    assertThat(builder.__Name().hasNone()).isTrue();
    assertThat(builder.withName("foo").__Name().value()).isEqualTo("foo");
  }

  
  @Test
  public void shouldImplementUnderscoreAccessorsForPrimitiveMaybes() {
    PrimitiveBeanBuilder builder = QB.builder(PrimitiveBeanBuilder.class);
    assertThat(builder.__I().hasNone()).isTrue();
    assertThat(builder.__D().hasNone()).isTrue();
    assertThat(builder.withI(42).__I().value()).isEqualTo(42);
    assertThat(builder.withD(1).__D().value()).isEqualTo(1);
  }

  
  @Test
  public void shouldConstructImmutableMixedValueTypes() {
    final MixedValueBuilder builder = QB.builder(MixedValueBuilder.class,
        new MixedValueGenerator());
    double[] ds = new double[] { 1.0d, 2.2d };
    float f = 1.3f;

    MixedValue actual = builder.withDs(ds).withF(f).withS("S").withSs("SS")
        .withLs(null).build();
    assertThat(actual.getDs()).isSameAs(ds);
    assertThat(actual.getF()).isEqualTo(f);
    assertThat(actual.getS()).isEqualTo("S");
    assertThat(actual.getSs()).isEqualTo("SS");
  }

  private Generator<IntegerValueBuilder,IntegerValue> intGenerator() {
    return new Generator<IntegerValueBuilder,IntegerValue>() {
      @Override
      public IntegerValue generate(final IntegerValueBuilder builder) {
        return new IntegerValue(builder._I());
      }
    };
  }

  @Test(expected = QuickBuilderError.class)
  public void shouldErrorWhenNoSetterForDeclaredPropery() {
    QB.builder(BuilderDeclaringNonExistingProperty.class);
  }

  @Test
  public void shouldNotErrorWhenNoSetterForDeclaredProperyButUnderscoreMethodExists() {
    try {
      QB.builder(PropertyOverridenByUnderscore.class);
      // pass
    } catch (Exception ex) {
      fail(ex.getMessage());
    }

  }

  @Test
  public void shouldAcceptBuilderInPlaceOfPropertyType() {
    CompositeBeanBuilder builder = QB.builder(CompositeBeanBuilder.class);
    FruitBuilder fb = QB.builder(FruitBuilder.class).withName("foo");
    CompositeBean actual = builder.withFruit(fb).build();
    assertThat(actual.getFruit().getName()).isEqualTo("foo");
  }

  @Test
  public void shouldAllowPropertyToBeOverriddenByBuilderAndType() {
    CompositeBeanBuilder builder = QB.builder(CompositeBeanBuilder.class);
    FruitBuilder fb = QB.builder(FruitBuilder.class).withName("foo");
    CompositeBean actual = builder.withFruit(fb).withFruit(new FruitBean())
        .build();
    // last call should take precedence
    assertThat(actual.getFruit().getName()).isNull();
  }

  @Test
  public void shouldAcceptBaseBuilderInterfaceInPlaceOfBuiltType() {
    BuilderDeclaringBaseBuilderProperty builder = QB
        .builder(BuilderDeclaringBaseBuilderProperty.class);
    FruitBuilder fb = QB.builder(FruitBuilder.class).withName("foo");
    CompositeBean bean = builder.withMoreFruit(fb).build();
    assertThat(bean.getMoreFruit().getName()).isEqualTo("foo");
  }
  
  
  
  @Test
  public void shouldCreateBridgeMethodsForErasedReturnTypes() {
    Apple a = FruitBuilders.anApple().withRipeness(2d).withLeaves(2).build();
    assertEquals(a.numberOfLeaves(),2);
  }

  @Test
  public void doesNotSupportWildCards() {
    try {
      QB.builder(BuilderDeclaringBoundedWildcardProperty.class);
    } catch (QuickBuilderError e) {
      assertThat(e).hasMessageContaining("wildcards not currently supported");
    }
  }

  @Test
  public void shouldThrowErrorWhenUnderScoreMethodHasParameter() {
    try {
      QB.builder(BuilderWithParameterisedUnderscoreMethod.class);
    } catch (QuickBuilderError e) {
      assertThat(e).hasMessageContaining("_Foo should not have parameters");
    }
  }

  @Test
  public void shouldThrowErrorWhenTypeOfUnderscoreMethodDoesNotMatchProperty() {
    try {
      QB.builder(BuilderWithTypeMismatchInUnderscoreMethod.class);
    } catch (QuickBuilderError e) {
      assertThat(e).hasMessageContaining("No setter found for Foo of type");
    }
  }

  @Test
  public void shouldThrowErrorWhenWithMethodDoesNotReturnBuilder() {
    try {
      QB.builder(BuilderWithPropertyReturningWrongType.class);
    } catch (QuickBuilderError e) {
      assertThat(e).hasMessageContaining(
          "should declare return type as "
              + BuilderWithPropertyReturningWrongType.class.getName());
    }
  }

  @Test
  public void shouldThrowErrorWhenWithMethodHasWrongNumberOfParameters() {
    try {
      QB.builder(BuilderWithPropertyWithTooManyParameters.class);
    } catch (QuickBuilderError e) {
      assertThat(e).hasMessageContaining("should take exactly one parameter");
    }
  }

  @Test
  public void shouldThrowErrorWhenPropertyAccessedWithoutAValueBeingSet() {
    final MixedValueBuilder builder = QB.builder(MixedValueBuilder.class,
        new MixedValueGenerator());
    try {
      builder.withF(1.0f).build();
    } catch (NoValueAvailableError e) {
      assertThat(e).hasMessageContaining("no value");
    }
    // fail
  }

  public static abstract class InvalidClass implements Builder<String> {

  }

  @Test
  public void shouldThrowErrorWhenAskedToImplementAClass() {
    try {
      QB.builder(InvalidClass.class);
      fail("expected an exception");
    } catch (QuickBuilderError e) {
      assertThat(e).hasMessageContaining("not an interface");
      assertThat(e).hasMessageContaining(InvalidClass.class.getName());
    }

  }

  
  interface Inaccessible extends Builder<String> {
    
  }
  
  @Test
  public void shouldThrowErrorWhenAskedToImplementInaccessibleInterface() {
    try {
      QB.builder(Inaccessible.class);
    } catch (QuickBuilderError e) {
      assertThat(e).hasMessageContaining(
          "Cannot implement the interface " + Inaccessible.class.getName());
    }
  }
  

  @Test
  public void shouldReturnConstantNextWhenNoChildrenSet() {
    FruitBuilder builder = QB.builder(FruitBuilder.class);
    assertThat(builder.next().hasSome()).isTrue();
  }
  
  @Test
  public void shouldReturnConstantNextWhenNoChildrenAreConstants() {
    FruitBuilder builder = QB.builder(FruitBuilder.class).withId("foo");
    assertThat(builder.next().hasSome()).isTrue();
    assertThat(builder.next().value().build().getId()).isEqualTo("foo");
  }
    
  @Test
  public void shouldReturnLimitedNextWhenChildrenAreLimited() {
    FruitBuilder builder = QB.builder(FruitBuilder.class).withId(new NonBuilder<String>());
    assertThat(builder.next().hasSome()).isFalse();
  }
  
  @Test
  public void shouldReturnNextStateWhenChildrenHaveTransistionableState() {
    FruitBuilder builder = QB.builder(FruitBuilder.class).withId(ElementSequence.from(Arrays.asList("a","b")));
    assertThat(builder.build().getId()).isEqualTo("a");
    assertThat(builder.next().value().build().getId()).isEqualTo("b");
  }
  
  @Test
  public void shouldReturnNoneWhenTransistionsExhausted() {
    FruitBuilder builder = QB.builder(FruitBuilder.class).withId(ElementSequence.from(Arrays.asList("a","b")));
    Maybe<Builder<FruitBean>> oneTransistion = builder.next();
    Maybe<Builder<FruitBean>> twoTransistions =  oneTransistion.value().next();
    assertThat(twoTransistions.hasSome()).isEqualTo(false);
  }
}
