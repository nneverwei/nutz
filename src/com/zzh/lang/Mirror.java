package com.zzh.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zzh.castor.Castors;
import com.zzh.castor.FailToCastObjectException;

public class Mirror<T> {

	private static class DefaultTypeExtractor implements TypeExtractor {

		@Override
		public Class<?>[] extract(Mirror<?> mirror) {
			ArrayList<Class<?>> re = new ArrayList<Class<?>>(5);
			re.add(mirror.getType());
			if (mirror.klass.isEnum())
				re.add(Enum.class);

			else if (mirror.klass.isArray())
				re.add(Array.class);

			else if (mirror.isString())
				re.add(String.class);

			else if (mirror.is(Class.class))
				re.add(Class.class);

			else if (mirror.is(Mirror.class))
				re.add(Mirror.class);

			else if (mirror.isStringLike())
				re.add(CharSequence.class);

			else if (mirror.isNumber()) {
				re.add(mirror.getType());
				re.add(Number.class);

			} else if (mirror.isBoolean())
				re.add(Boolean.class);

			else if (mirror.isChar())
				re.add(Character.class);

			else if (mirror.isOf(Map.class))
				re.add(Map.class);

			else if (mirror.isOf(Collection.class))
				re.add(Collection.class);

			else if (mirror.isOf(Calendar.class))
				re.add(Calendar.class);

			else if (mirror.isOf(Timestamp.class))
				re.add(Timestamp.class);

			else if (mirror.isOf(java.sql.Date.class))
				re.add(java.sql.Date.class);

			else if (mirror.isOf(java.sql.Time.class))
				re.add(java.sql.Time.class);

			else if (mirror.isOf(java.util.Date.class))
				re.add(java.util.Date.class);

			re.add(Object.class);
			return re.toArray(new Class<?>[re.size()]);
		}

	}

	private final static DefaultTypeExtractor defaultTypeExtractor = new DefaultTypeExtractor();

	public static <T> Mirror<T> me(Class<T> classOfT) {
		return null == classOfT ? null : new Mirror<T>(classOfT)
				.setTypeExtractor(defaultTypeExtractor);
	}

	public static <T> Mirror<T> me(Class<T> classOfT, TypeExtractor typeExtractor) {
		return null == classOfT ? null : new Mirror<T>(classOfT)
				.setTypeExtractor(typeExtractor == null ? defaultTypeExtractor : typeExtractor);
	}

	private Class<T> klass;

	private TypeExtractor typeExtractor;

	public Mirror<T> setTypeExtractor(TypeExtractor typeExtractor) {
		this.typeExtractor = typeExtractor;
		return this;
	}

	private Mirror(Class<T> classOfT) {
		klass = classOfT;
	}

	public Method getGetter(String fieldName) throws NoSuchMethodException {
		try {
			String fn = Strings.capitalize(fieldName);
			try {
				try {
					return klass.getMethod("get" + fn);
				} catch (NoSuchMethodException e) {
					Method m = klass.getMethod("is" + fn);
					if (!Mirror.me(m.getReturnType()).isBoolean())
						throw new NoSuchMethodException();
					return m;
				}
			} catch (NoSuchMethodException e) {
				return klass.getMethod(fieldName);
			}
		} catch (Exception e) {
			throw Lang.makeThrow(NoSuchMethodException.class, "Fail to find getter for [%s]->[%s]",
					klass.getName(), fieldName);
		}
	}

	public Method getGetter(Field field) throws NoSuchMethodException {
		try {
			try {
				String fn = Strings.capitalize(field.getName());
				if (Mirror.me(field.getType()).is(boolean.class))
					return klass.getMethod("is" + fn);
				else
					return klass.getMethod("get" + fn);
			} catch (NoSuchMethodException e) {
				return klass.getMethod(field.getName());
			}
		} catch (Exception e) {
			throw Lang.makeThrow(NoSuchMethodException.class, "Fail to find getter for [%s]->[%s]",
					klass.getName(), field.getName());
		}
	}

	public Method getSetter(Field field) throws NoSuchMethodException {
		try {
			try {
				return klass
						.getMethod("set" + Strings.capitalize(field.getName()), field.getType());
			} catch (Exception e) {
				try {
					if (field.getName().startsWith("is")
							&& Mirror.me(field.getType()).is(boolean.class))
						return klass.getMethod("set" + field.getName().substring(2), field
								.getType());
					return klass.getMethod(field.getName(), field.getType());
				} catch (Exception e1) {
					return klass.getMethod(field.getName(), field.getType());
				}
			}
		} catch (Exception e) {
			throw Lang.makeThrow(NoSuchMethodException.class, "Fail to find setter for [%s]->[%s]",
					klass.getName(), field.getName());
		}
	}

	public Method getSetter(String fieldName, Class<?> paramType) throws NoSuchMethodException {
		try {
			try {
				return klass.getMethod("set" + Strings.capitalize(fieldName), paramType);
			} catch (Exception e) {
				return klass.getMethod(fieldName, paramType);
			}
		} catch (Exception e) {
			throw Lang.makeThrow(NoSuchMethodException.class,
					"Fail to find setter for [%s]->[%s(%s)]", klass.getName(), fieldName, paramType
							.getName());
		}
	}

	public Method[] findSetters(String fieldName) {
		String mName = "set" + Strings.capitalize(fieldName);
		ArrayList<Method> ms = new ArrayList<Method>();
		for (Method m : getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length != 1)
				continue;
			if (m.getName().equals(mName)) {
				ms.add(m);
			}
		}
		return ms.toArray(new Method[ms.size()]);
	}

	public Field getField(String name) throws NoSuchFieldException {
		Class<?> theClass = klass;
		Field f;
		while (null != theClass && !(theClass == Object.class)) {
			try {
				f = theClass.getDeclaredField(name);
				return f;
			} catch (NoSuchFieldException e) {
				theClass = theClass.getSuperclass();
			}
		}
		throw new NoSuchFieldException(String.format(
				"Can NO find field [%s] in class [%s] and it's parents classes", name, klass
						.getName()));
	}

	public <AT extends Annotation> Field getField(Class<AT> ann) throws NoSuchFieldException {
		for (Field field : this.getFields()) {
			if (null != field.getAnnotation(ann))
				return field;
		}
		throw new NoSuchFieldException(String.format(
				"Can NO find field [@%s] in class [%s] and it's parents classes", ann.getName(),
				klass.getName()));
	}

	private static boolean isIgnoredField(Field f) {
		if (Modifier.isStatic(f.getModifiers()))
			return true;
		if (Modifier.isFinal(f.getModifiers()))
			return true;
		if (f.getName().startsWith("this$"))
			return true;
		return false;
	}

	public Field[] getFields() {
		Class<?> theClass = klass;
		LinkedList<Field> list = new LinkedList<Field>();
		while (null != theClass && !(theClass == Object.class)) {
			Field[] fs = theClass.getDeclaredFields();
			for (int i = 0; i < fs.length; i++) {
				if (isIgnoredField(fs[i]))
					continue;
				list.add(fs[i]);
			}
			theClass = theClass.getSuperclass();
		}
		return list.toArray(new Field[list.size()]);
	}

	public Method[] getMethods() {
		Class<?> theClass = klass;
		LinkedList<Method> list = new LinkedList<Method>();
		while (null != theClass && !(theClass == Object.class)) {
			Method[] fs = theClass.getMethods();
			for (int i = 0; i < fs.length; i++) {
				list.add(fs[i]);
			}
			theClass = theClass.getSuperclass();
		}
		return list.toArray(new Method[list.size()]);
	}

	public Method[] getStaticMethods() {
		List<Method> list = new LinkedList<Method>();
		for (Method m : klass.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()))
				list.add(m);
		}
		return list.toArray(new Method[list.size()]);
	}

	private static RuntimeException makeSetValueException(Class<?> type, String name, Object value,
			Exception e) {
		return new FailToSetValueException(String.format(
				"Fail to set value [%s] to [%s]->[%s] because '%s'", value, type.getName(), name, e
						.getMessage()));
	}

	public void setValue(Object obj, Field field, Object value) throws FailToSetValueException {
		if (!field.isAccessible())
			field.setAccessible(true);
		Mirror<?> me = Mirror.me(field.getType());
		if (null != value)
			try {
				if (!Mirror.me(value.getClass()).canCastToDirectly(me.getType()))
					value = Castors.me().castTo(value, field.getType());
			} catch (FailToCastObjectException e) {
				throw makeSetValueException(obj.getClass(), field.getName(), value, e);
			}
		else {
			if (me.isNumber())
				value = 0;
			else if (me.isChar())
				value = (char) 0;
		}
		try {
			field.set(obj, value);
		} catch (Exception e) {
			throw makeSetValueException(obj.getClass(), field.getName(), value, e);
		}
	}

	public void setValue(Object obj, String fieldName, Object value) throws FailToSetValueException {
		try {
			this.getSetter(fieldName, value.getClass()).invoke(obj, value);
		} catch (Exception e) {
			try {
				Field field = this.getField(fieldName);
				setValue(obj, field, value);
			} catch (Exception e1) {
				throw makeSetValueException(obj.getClass(), fieldName, value, e1);
			}
		}
	}

	private static RuntimeException makeGetValueException(Class<?> type, String name) {
		return new FailToGetValueException(String.format("Fail to get value for [%s]->[%s]", type
				.getName(), name));
	}

	public Object getValue(Object obj, Field f) throws FailToGetValueException {
		try {
			if (!f.isAccessible())
				f.setAccessible(true);
			return f.get(obj);
		} catch (Exception e) {
			throw makeGetValueException(obj.getClass(), f.getName());
		}
	}

	public Object getValue(Object obj, String name) throws FailToGetValueException {
		try {
			return this.getGetter(name).invoke(obj);
		} catch (Exception e) {
			try {
				Field f = getField(name);
				return getValue(obj, f);
			} catch (NoSuchFieldException e1) {
				throw makeGetValueException(obj.getClass(), name);
			}
		}
	}

	public Class<T> getType() {
		return klass;
	}

	public Class<?>[] extractTypes() {
		return typeExtractor.extract(this);
	}

	public Class<?> getWrpperClass() {
		if (!klass.isPrimitive()) {
			if (this.isPrimitiveNumber() || this.is(Boolean.class) || this.is(Character.class))
				return klass;
			throw Lang.makeThrow("Class '%s' should be a primitive class", klass.getName());
		}
		if (is(int.class))
			return Integer.class;
		if (is(char.class))
			return Character.class;
		if (is(boolean.class))
			return Boolean.class;
		if (is(long.class))
			return Long.class;
		if (is(float.class))
			return Float.class;
		if (is(byte.class))
			return Byte.class;
		if (is(short.class))
			return Short.class;
		if (is(double.class))
			return Double.class;

		throw Lang.makeThrow("Class [%s] has no wrapper class!", klass.getName());
	}

	public Class<?> getOuterClass() {
		if (Modifier.isStatic(klass.getModifiers()))
			return null;
		String name = klass.getName();
		int pos = name.lastIndexOf('$');
		if (pos == -1)
			return null;
		name = name.substring(0, pos);
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public Borning<T> getBorning(Object... args) {
		return new Borning<T>(this, args);
	}

	public T born(Object... args) {
		return this.getBorning(args).born();
	}

	private static boolean doMatchMethodParamsType(Class<?>[] paramTypes, Class<?>[] methodArgTypes) {
		if (paramTypes.length == 0 && methodArgTypes.length == 0)
			return true;
		if (paramTypes.length == methodArgTypes.length) {
			for (int i = 0; i < paramTypes.length; i++)
				if (!Mirror.me(paramTypes[i]).canCastToDirectly((methodArgTypes[i])))
					return false;
			return true;
		} else if (paramTypes.length + 1 == methodArgTypes.length) {
			if (!methodArgTypes[paramTypes.length].isArray())
				return false;
			for (int i = 0; i < paramTypes.length; i++)
				if (!Mirror.me(paramTypes[i]).canCastToDirectly((methodArgTypes[i])))
					return false;
			return true;
		}
		return false;
	}

	public <A> Object invoke(Object obj, String methodName, A... args) {
		try {
			Method m = klass.getMethod(methodName, args.getClass());
			return m.invoke(obj, (Object) args);
		} catch (Exception e1) {
			Class<?>[] paramTypes = new Class<?>[null == args ? 0 : args.length];
			for (int i = 0; i < paramTypes.length; i++)
				paramTypes[i] = args[i].getClass();
			try {
				return klass.getMethod(methodName, paramTypes).invoke(obj, args);
			} catch (Exception e2) {
				try {
					return findMethod(methodName, paramTypes).invoke(obj, args);
				} catch (Exception e) {
					throw Lang.wrapThrow(e);
				}
			}
		}
	}

	public Method findMethod(String name, Class<?>... paramTypes) throws NoSuchMethodException {
		try {
			return klass.getMethod(name, paramTypes);
		} catch (NoSuchMethodException e) {
			for (Method m : klass.getMethods()) {
				if (m.getName().equals(name))
					if (doMatchMethodParamsType(paramTypes, m.getParameterTypes()))
						return m;
			}
		}
		throw new NoSuchMethodException(String.format("Method %s->%s with params:\n%s", klass
				.getName(), name, paramTypes));
	}

	public Method findMethod(Class<?> returnType, Class<?>... paramTypes)
			throws NoSuchMethodException {
		for (Method m : klass.getMethods()) {
			if (returnType == m.getReturnType())
				if (paramTypes.length == m.getParameterTypes().length) {
					boolean noThisOne = false;
					for (int i = 0; i < paramTypes.length; i++) {
						if (paramTypes[i] != m.getParameterTypes()[i]) {
							noThisOne = true;
							break;
						}
					}
					if (!noThisOne)
						return m;
				}
		}
		throw new NoSuchMethodException(String.format(
				"Can not find method in [%s] with return type '%s' and arguemtns \n'%s'!", klass
						.getName(), returnType.getName(), paramTypes));

	}

	public static MatchType matchMethodParamsType(Class<?>[] methodArgTypes, Object... args) {
		int len = args == null ? 0 : args.length;
		if (len == 0 && methodArgTypes.length == 0)
			return MatchType.YES;
		if (methodArgTypes.length == len) {
			for (int i = 0; i < len; i++)
				if (!Mirror.me(args[i].getClass()).canCastToDirectly((methodArgTypes[i])))
					return MatchType.NO;
			return MatchType.YES;
		} else if (len + 1 == methodArgTypes.length) {
			if (!methodArgTypes[len].isArray())
				return MatchType.NO;
			for (int i = 0; i < len; i++)
				if (!Mirror.me(args[i].getClass()).canCastToDirectly((methodArgTypes[i])))
					return MatchType.NO;
			return MatchType.LACK;
		}
		return MatchType.NO;
	}

	// @SuppressWarnings("unchecked")
	// public T duplicate(T src) {
	// Method m;
	// try {
	// m = klass.getMethod("clone");
	// return (T) m.invoke(src);
	// } catch (Exception failToClone) {
	// try {
	// T obj = born();
	// Field[] fields = getFields();
	// for (Field field : fields) {
	// Object value = getValue(src, field);
	// setValue(obj, field, value);
	// }
	// return obj;
	//
	// } catch (Exception e) {
	// throw Lang.wrapThrow(e);
	// }
	// }
	// }

	public boolean is(Class<?> type) {
		if (null == type)
			return false;
		if (klass == type)
			return true;
		return false;
	}

	public boolean is(String className) {
		return klass.getName().equals(className);
	}

	public boolean isOf(Class<?> type) {
		return type.isAssignableFrom(klass);
	}

	public boolean isString() {
		return is(String.class);
	}

	public boolean isStringLike() {
		return CharSequence.class.isAssignableFrom(klass);
	}

	public boolean isChar() {
		return is(char.class) || is(Character.class);
	}

	public boolean isEnum() {
		return klass.isEnum();
	}

	public boolean isBoolean() {
		return is(boolean.class) || is(Boolean.class);
	}

	public boolean isFloat() {
		return is(float.class) || is(Float.class);
	}

	public boolean isDouble() {
		return is(double.class) || is(Double.class);
	}

	public boolean isInt() {
		return is(int.class) || is(Integer.class);
	}

	public boolean isInteger() {
		return isInt() || isLong() || isShort() || isByte();
	}

	public boolean isDecimal() {
		return isFloat() || isDouble();
	}

	public boolean isLong() {
		return is(long.class) || is(Long.class);
	}

	public boolean isShort() {
		return is(short.class) || is(Short.class);
	}

	public boolean isByte() {
		return is(byte.class) || is(Byte.class);
	}

	public boolean isWrpperOf(Class<?> type) {
		try {
			return Mirror.me(type).getWrpperClass() == klass;
		} catch (Exception e) {}
		return false;
	}

	public boolean canCastToDirectly(Class<?> type) {
		if (klass == type)
			return true;
		if (type.isAssignableFrom(klass))
			return true;
		if (klass.isPrimitive() && type.isPrimitive()) {
			if (this.isPrimitiveNumber() && Mirror.me(type).isPrimitiveNumber())
				return true;
		}
		try {
			return Mirror.me(type).getWrpperClass() == this.getWrpperClass();
		} catch (Exception e) {}
		return false;
	}

	public boolean isPrimitiveNumber() {
		return isInt() || isLong() || isFloat() || isDouble() || isByte() || isShort();
	}

	public boolean isNumber() {
		return Number.class.isAssignableFrom(klass) || klass.isPrimitive() && !is(boolean.class)
				&& !is(char.class);
	}

	public boolean isDateTimeLike() {
		return Calendar.class.isAssignableFrom(klass)
				|| java.util.Date.class.isAssignableFrom(klass)
				|| java.sql.Timestamp.class.isAssignableFrom(klass)
				|| java.sql.Date.class.isAssignableFrom(klass)
				|| java.sql.Time.class.isAssignableFrom(klass);
	}

	public static Type[] getTypeParams(Class<?> klass) {
		Type superclass = klass.getGenericSuperclass();
		if (superclass instanceof Class) {
			throw new RuntimeException("Missing type parameter.");
		}
		return ((ParameterizedType) superclass).getActualTypeArguments();
	}

	public static enum MatchType {
		YES, LACK, NO
	}
}