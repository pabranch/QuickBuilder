package org.pitest.quickbuilder.internal;

import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.pitest.quickbuilder.Builder;
import org.pitest.quickbuilder.Generator;
import org.pitest.quickbuilder.SequenceBuilder;
import org.pitest.quickbuilder.common.ConstantBuilder;

class BuilderBuilder {

  private static final TypeName CONSTANT_BUILDER = TypeName
                                                         .fromClass(ConstantBuilder.class);
  private static final String   GENERATOR_FIELD      = "___generator";
  private static final TypeName BUILDER_INTERFACE    = TypeName
                                                         .fromClass(Builder.class);
  private static final TypeName GENERATOR            = TypeName
                                                         .fromClass(Generator.class);

  private static final TypeName SequenceBuilder      = TypeName
                                                         .fromClass(SequenceBuilder.class);

  private final String          builderName;
  private final String          proxiedName;
  private final String          built;
  private final List<Property>  ps;

  BuilderBuilder(final String builderName, final String proxiedName,
      final String built, final List<Property> ps) {
    this.builderName = builderName;
    this.proxiedName = proxiedName;
    this.built = built;
    this.ps = ps;
  }

  public byte[] build() throws Exception {

    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

    cw.visit(Opcodes.V1_5, ACC_PUBLIC + ACC_SUPER, this.builderName,
        "Ljava/lang/Object;L" + BUILDER_INTERFACE.name() + "<L" + this.built
            + ";>;" + "L" + this.proxiedName + ";", "java/lang/Object",
        new String[] { BUILDER_INTERFACE.name(), SequenceBuilder.name(),
            this.proxiedName });

    createFields(cw);

    createInitMethod(cw);
    if (!this.uniqueProperties().isEmpty()) {
      createCopyConstructor(cw);
    }

    createPropertyMethods(cw);

    createBuildMethod(cw);
    createBridgeForBuildMethod(cw);

    createHasNextMethod(cw);
    createNextMethod(cw);
    createSequenceBuildMethod(cw);
    createBuildAllMethod(cw);
    createLimitMethod(cw);
    createIteratorMethod(cw);

    cw.visitEnd();

    final byte[] bs = cw.toByteArray();

    return bs;

  }

  private void createNextMethod(final ClassWriter cw) {
    final MethodVisitor mv = cw
        .visitMethod(
            ACC_PUBLIC,
            "next",
            "()Lorg/pitest/quickbuilder/Maybe;",
            "()Lorg/pitest/quickbuilder/Maybe<Lorg/pitest/quickbuilder/Builder<TT;>;>;",
            null);
    mv.visitCode();

    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, this.builderName, "hasNext", "()Z", false);
    final Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);

    mv.visitVarInsn(ALOAD, 0);

    mv.visitTypeInsn(NEW, this.builderName);
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, GENERATOR_FIELD,
        GENERATOR.type());

    for (final Property each : this.uniqueProperties()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
          "Lorg/pitest/quickbuilder/Builder;");

      final Label nullCheck = new Label();
      mv.visitJumpInsn(IFNULL, nullCheck);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
          "Lorg/pitest/quickbuilder/Builder;");

      mv.visitMethodInsn(INVOKEINTERFACE, "org/pitest/quickbuilder/Builder",
          "next", "()Lorg/pitest/quickbuilder/Maybe;", true);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/pitest/quickbuilder/Maybe",
          "value", "()Ljava/lang/Object;", false);
      mv.visitTypeInsn(CHECKCAST, "org/pitest/quickbuilder/Builder");
      final Label propHandled = new Label();
      mv.visitJumpInsn(GOTO, propHandled);
      mv.visitLabel(nullCheck);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
          "Lorg/pitest/quickbuilder/Builder;");
      mv.visitLabel(propHandled);

    }

    mv.visitMethodInsn(INVOKESPECIAL, this.builderName, "<init>",
        this.initDescriptor(), false);

    mv.visitMethodInsn(INVOKESTATIC, "org/pitest/quickbuilder/Maybe", "some",
        "(Ljava/lang/Object;)Lorg/pitest/quickbuilder/Maybe;", false);
    mv.visitInsn(ARETURN);

    mv.visitLabel(l1);
    mv.visitMethodInsn(INVOKESTATIC, "org/pitest/quickbuilder/Maybe", "none",
        "()Lorg/pitest/quickbuilder/Maybe$None;", false);
    mv.visitInsn(ARETURN);

    mv.visitMaxs(1, 1);
    mv.visitEnd();

  }

  private void createPropertyMethods(final ClassWriter cw) {

    for (final Property each : this.ps) {
      createWithMethod(cw, each);
    }

    for (final Property each : this.uniqueProperties()) {
      createAccessor(cw, each);
      createMaybeProperty(cw, each);
    }
  }

  private void createCopyConstructor(final ClassWriter cw) {

    final String sig = copyConstructorSignature();
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>",
        initDescriptor(), sig, null);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
        false);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, this.builderName, GENERATOR_FIELD,
        GENERATOR.type());

    int index = 2;
    for (final Property each : this.uniqueProperties()) {
      index = storeParameterInField(mv, index, each);
    }

    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private int storeParameterInField(final MethodVisitor mv, final int index,
      final Property each) {
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, index);
    mv.visitMethodInsn(INVOKESTATIC,
        "org/pitest/quickbuilder/internal/BuilderImplementation",
        "copyBuilder",
        "(Lorg/pitest/quickbuilder/Builder;)Lorg/pitest/quickbuilder/Builder;",
        false);
    mv.visitFieldInsn(PUTFIELD, this.builderName, each.name(),
        "Lorg/pitest/quickbuilder/Builder;");
    return index + 1;
  }

  private String copyConstructorSignature() {
    final StringBuilder sb = new StringBuilder();
    sb.append("(");
    sb.append("Lorg/pitest/quickbuilder/Generator<" + this.built + ";L"
        + this.builderName + ";>;");

    for (final Property each : this.uniqueProperties()) {
      sb.append("Lorg/pitest/quickbuilder/Builder<" + each.declaredType()
          + ">;");
    }

    sb.append(";)V");
    final String sig = sb.toString();
    return sig;
  }

  private String initDescriptor() {
    return "("
        + GENERATOR.type()
        + StringUtils.repeat("Lorg/pitest/quickbuilder/Builder;", this
            .uniqueProperties().size()) + ")V";
  }

  private void createAccessor(final ClassWriter cw, final Property each) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "_" + each.name(), "()"
        + each.type(), null, null);
    mv.visitCode();

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
        "Lorg/pitest/quickbuilder/Builder;");

    final Label l = new Label();
    mv.visitJumpInsn(IFNONNULL, l);
    mv.visitTypeInsn(NEW, "org/pitest/quickbuilder/NoValueAvailableError");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("_" + each.name()
        + "() called, but no value has been set for property " + each.name());
    mv.visitMethodInsn(INVOKESPECIAL,
        "org/pitest/quickbuilder/NoValueAvailableError", "<init>",
        "(Ljava/lang/String;)V", false);
    mv.visitInsn(ATHROW);

    mv.visitLabel(l);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
        "Lorg/pitest/quickbuilder/Builder;");

    mv.visitMethodInsn(INVOKEINTERFACE, "org/pitest/quickbuilder/Builder",
        "build", "()Ljava/lang/Object;", true);

    castPrimitives(each, mv);

    mv.visitInsn(each.returnOp());
    mv.visitMaxs(1, 1);
    mv.visitEnd();

  }

  private void castPrimitives(final Property each, final MethodVisitor mv) {
    if (each.getSort() == Type.INT) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I",
          false);
    } else if (each.getSort() == Type.BOOLEAN) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue",
          "()Z", false);
    } else if (each.getSort() == Type.BYTE) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B",
          false);
    } else if (each.getSort() == Type.CHAR) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue",
          "()C", false);
    } else if (each.getSort() == Type.DOUBLE) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue",
          "()D", false);
    } else if (each.getSort() == Type.FLOAT) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F",
          false);
    } else if (each.getSort() == Type.LONG) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J",
          false);
    } else if (each.getSort() == Type.SHORT) {
      mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S",
          false);
    } else {
      mv.visitTypeInsn(CHECKCAST, each.typeName());
    }
  }

  private void createInitMethod(final ClassWriter cw) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "("
        + GENERATOR.type() + ")V", "(L" + GENERATOR.name() + "<L" + this.built
        + ";+L" + BUILDER_INTERFACE.name() + "<L" + this.built + ";>;>;)V",
        null);
    mv.visitCode();

    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
        false);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, this.builderName, GENERATOR_FIELD,
        GENERATOR.type());

    for (final Property each : this.uniqueProperties()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ACONST_NULL);
      mv.visitFieldInsn(PUTFIELD, this.builderName, each.name(),
          "Lorg/pitest/quickbuilder/Builder;");
    }

    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

  }

  private void createFields(final ClassWriter cw) {
    final FieldVisitor fv1 = cw.visitField(ACC_PRIVATE + ACC_FINAL,
        GENERATOR_FIELD, GENERATOR.type(), "L" + GENERATOR.name() + "<L"
            + this.built + ";L" + this.builderName + ";>;", null);
    fv1.visitEnd();

    final Set<Property> uniquePs = uniqueProperties();
    for (final Property each : uniquePs) {
      final FieldVisitor fv = cw.visitField(fieldFlags(), each.name(),
          BUILDER_INTERFACE.type(),
          "L" + BUILDER_INTERFACE.name() + "<" + each.type() + ";>;", null);
      fv.visitEnd();
    }
  }

  private int fieldFlags() {
    return ACC_PRIVATE + ACC_FINAL;
  }

  private Set<Property> uniqueProperties() {
    final Set<Property> uniquePs = new LinkedHashSet<Property>(this.ps);
    return uniquePs;
  }

  private void createWithMethod(final ClassWriter cw, final Property prop) {
    createImmutableWithMethod(cw, prop);

    if (prop.needsBridge()) {
      createBridge(prop, cw);
    }
  }

  private void createBridge(final Property prop, final ClassWriter cw) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE
        + ACC_SYNTHETIC, prop.withMethodName(), "(" + prop.declaredType() + ")"
        + prop.bridgeReturnType().getDescriptor(), null, null);

    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(prop.loadIns(), 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, this.builderName, prop.withMethodName(),
        "(" + prop.declaredType() + ")L" + this.proxiedName + ";", false);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(3, 3);
    mv.visitEnd();

  }

  private void createImmutableWithMethod(final ClassWriter cw,
      final Property prop) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, prop.withMethodName(),
        "(" + prop.declaredType() + ")L" + this.proxiedName + ";", null, null);
    mv.visitCode();
    mv.visitTypeInsn(NEW, this.builderName);
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, GENERATOR_FIELD,
        GENERATOR.type());

    for (final Property each : this.uniqueProperties()) {
      if (each.name().equals(prop.name())) {
        if (!prop.isBuilder()) {
          wrapInBuilderObject(prop, mv);
        } else {
          mv.visitVarInsn(ALOAD, 1);
        }
      } else {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
            "Lorg/pitest/quickbuilder/Builder;");
      }
    }

    mv.visitMethodInsn(INVOKESPECIAL, this.builderName, "<init>",
        initDescriptor(), false);
    mv.visitInsn(ARETURN);

    mv.visitMaxs(1, 1);
    mv.visitEnd();

  }

  private void wrapInBuilderObject(final Property prop, final MethodVisitor mv) {
    mv.visitTypeInsn(NEW, CONSTANT_BUILDER.name());
    mv.visitInsn(DUP);
    mv.visitVarInsn(prop.loadIns(), 1);

    convertPrimitiveToWrappingObject(prop, mv);

    mv.visitMethodInsn(INVOKESPECIAL, CONSTANT_BUILDER.name(), "<init>",
        "(Ljava/lang/Object;)V", false);
  }

  private void convertPrimitiveToWrappingObject(final Property prop,
      final MethodVisitor mv) {
    if (prop.getSort() == Type.INT) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
          "(I)Ljava/lang/Integer;", false);
    } else if (prop.getSort() == Type.BOOLEAN) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
          "(Z)Ljava/lang/Boolean;", false);
    } else if (prop.getSort() == Type.BYTE) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf",
          "(B)Ljava/lang/Byte;", false);
    } else if (prop.getSort() == Type.SHORT) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf",
          "(S)Ljava/lang/Short;", false);
    } else if (prop.getSort() == Type.DOUBLE) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf",
          "(D)Ljava/lang/Double;", false);
    } else if (prop.getSort() == Type.FLOAT) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf",
          "(F)Ljava/lang/Float;", false);
    } else if (prop.getSort() == Type.LONG) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf",
          "(J)Ljava/lang/Long;", false);
    } else if (prop.getSort() == Type.CHAR) {
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf",
          "(C)Ljava/lang/Character;", false);
    }
  }

  private void createBuildMethod(final ClassWriter cw) {
    MethodVisitor mv;
    mv = cw.visitMethod(ACC_PUBLIC, "build", "()L" + this.built + ";", null,
        null);
    mv.visitCode();

    // handle generator case
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, GENERATOR_FIELD,
        GENERATOR.type());
    final Label defaultConsCall = new Label();
    mv.visitJumpInsn(IFNULL, defaultConsCall);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, GENERATOR_FIELD,
        GENERATOR.type());
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEINTERFACE, GENERATOR.name(), "generate", "("
        + BUILDER_INTERFACE.type() + ")Ljava/lang/Object;", true);
    mv.visitTypeInsn(CHECKCAST, this.built);
    mv.visitVarInsn(ASTORE, 1);
    final Label setProps = new Label();
    mv.visitJumpInsn(GOTO, setProps);

    mv.visitLabel(defaultConsCall);
    mv.visitTypeInsn(Opcodes.NEW, this.built);
    mv.visitInsn(Opcodes.DUP);
    mv.visitMethodInsn(INVOKESPECIAL, this.built, "<init>", "()V", false);
    mv.visitVarInsn(ASTORE, 1);

    mv.visitLabel(setProps);
    for (final Property p : this.uniqueProperties()) {
      if (p.isHasSetter()) {
        callSetterIfPropertyHasValue(mv, p);
      }
    }

    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(1, 1); // irrelevent
    mv.visitEnd();
  }

  private void callSetterIfPropertyHasValue(final MethodVisitor mv,
      final Property p) {
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, p.name(),
        "Lorg/pitest/quickbuilder/Builder;");
    final Label l = new Label();
    mv.visitJumpInsn(IFNULL, l);

    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, this.builderName, p.name(),
        "Lorg/pitest/quickbuilder/Builder;");

    mv.visitMethodInsn(INVOKEINTERFACE, BUILDER_INTERFACE.name(), "build",
        "()Ljava/lang/Object;", true);

    castPrimitives(p, mv);

    mv.visitMethodInsn(INVOKEVIRTUAL, this.built, p.setter().name(), p.setter()
        .desc(), false);

    mv.visitLabel(l);
  }

  private void createBridgeForBuildMethod(final ClassWriter cw) {

    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE
        + ACC_SYNTHETIC, "build", "()Ljava/lang/Object;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, this.builderName, "build", "()L"
        + this.built + ";", false);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void createMaybeProperty(final ClassWriter cw, final Property each) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "__" + each.name(),
        "()Lorg/pitest/quickbuilder/Maybe;",
        "()Lorg/pitest/quickbuilder/Maybe<" + each.type() + ">;", null);

    mv.visitCode();
    final Label l0 = new Label();
    final Label l1 = new Label();
    final Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2,
        "org/pitest/quickbuilder/QuickBuilderError");
    mv.visitLabel(l0);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, this.builderName, "_" + each.name(), "()"
        + each.type(), false);
    convertPrimitiveToWrappingObject(each, mv);

    mv.visitMethodInsn(INVOKESTATIC, "org/pitest/quickbuilder/Maybe", "some",
        "(Ljava/lang/Object;)Lorg/pitest/quickbuilder/Maybe;", false);
    mv.visitLabel(l1);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l2);

    mv.visitVarInsn(ASTORE, 1);

    mv.visitMethodInsn(INVOKESTATIC, "org/pitest/quickbuilder/Maybe", "none",
        "()Lorg/pitest/quickbuilder/Maybe$None;", false);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(1, 2);
    mv.visitEnd();

  }

  private void createHasNextMethod(final ClassWriter cw) {

    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hasNext", "()Z", null,
        null);

    mv.visitCode();
    final Label l1 = new Label();

    for (final Property each : this.uniqueProperties()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
          "Lorg/pitest/quickbuilder/Builder;");
      final Label nullCheck = new Label();
      mv.visitJumpInsn(IFNULL, nullCheck);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, this.builderName, each.name(),
          "Lorg/pitest/quickbuilder/Builder;");
      mv.visitMethodInsn(INVOKEINTERFACE, "org/pitest/quickbuilder/Builder",
          "next", "()Lorg/pitest/quickbuilder/Maybe;", true);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/pitest/quickbuilder/Maybe",
          "hasSome", "()Z", false);
      mv.visitJumpInsn(IFEQ, l1);
      mv.visitLabel(nullCheck);
    }

    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

  }

  private void createSequenceBuildMethod(final ClassWriter cw) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "build",
        "(I)Ljava/util/List;", "(I)Ljava/util/List<L" + this.built + ";>;",
        null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC,
        "org/pitest/quickbuilder/common/Sequences", "build",
        "(Lorg/pitest/quickbuilder/Builder;I)Ljava/util/List;", false);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }

  private void createBuildAllMethod(final ClassWriter cw) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "buildAll",
        "()Ljava/util/List;", "()Ljava/util/List<L" + this.built + ";>;", null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC,
        "org/pitest/quickbuilder/common/Sequences", "buildAll",
        "(Lorg/pitest/quickbuilder/Builder;)Ljava/util/List;", false);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }

  private void createLimitMethod(final ClassWriter cw) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "limit",
        "(I)" + SequenceBuilder.type(),
        "(I)L" + SequenceBuilder.name() + "<TT;>;", null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitMethodInsn(
        INVOKESTATIC,
        "org/pitest/quickbuilder/common/Sequences",
        "limit",
        "(Lorg/pitest/quickbuilder/Builder;I)" + SequenceBuilder.type(),
        false);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }


  private void createIteratorMethod(final ClassWriter cw) {
    final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "iterator", "()Ljava/util/Iterator;", "()Ljava/util/Iterator<TT;>;", null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESTATIC, "org/pitest/quickbuilder/common/Sequences", "iterator", "(Lorg/pitest/quickbuilder/Builder;)Ljava/util/Iterator;", false);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();   
  }
}
