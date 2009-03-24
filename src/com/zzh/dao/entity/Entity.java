package com.zzh.dao.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zzh.dao.entity.annotation.*;
import com.zzh.lang.Lang;
import com.zzh.lang.Mirror;

public class Entity<T> {

	Entity() {
		manys = new HashMap<String, Link>();
		ones = new HashMap<String, Link>();
		manyManys = new HashMap<String, Link>();
	}

	private Map<String, EntityField> fieldMapping;
	Mirror<T> mirror;

	private static interface Borning<T> {
		T born(ResultSet rs) throws Exception;
	}

	static abstract class ReflectBorning<T> implements Borning<T> {
		Entity<T> entity;

		ReflectBorning(Entity<T> entity) {
			this.entity = entity;
		}

		abstract T create() throws Exception;

		public T born(ResultSet rs) throws Exception {
			T obj = create();
			Iterator<EntityField> it = entity.fields().iterator();
			while (it.hasNext()) {
				EntityField ef = it.next();
				ef.fillFieldValueFromResultSet(obj, rs);
			}
			return obj;
		}
	}

	static class DefaultConstructorBorning<T> extends ReflectBorning<T> {
		Constructor<T> c;

		public DefaultConstructorBorning(Entity<T> entity, Constructor<T> c) {
			super(entity);
			this.c = c;
		}

		public T create() throws Exception {
			return c.newInstance();
		}
	}

	static class DefaultStaticMethodBorning<T> extends ReflectBorning<T> {
		Method method;

		public DefaultStaticMethodBorning(Entity<T> entity, Method defMethod) {
			super(entity);
			this.method = defMethod;
		}

		@SuppressWarnings("unchecked")
		public T create() throws Exception {
			return (T) method.invoke(null);
		}
	}

	static class StaticResultSetMethodBorning<T> implements Borning<T> {
		Method method;

		StaticResultSetMethodBorning(Method rsMethod) {
			this.method = rsMethod;
		}

		@SuppressWarnings("unchecked")
		public T born(ResultSet rs) throws Exception {
			return (T) method.invoke(null, rs);
		}
	}

	static class ResultSetConstructorBorning<T> implements Borning<T> {
		Constructor<T> c;

		public ResultSetConstructorBorning(Constructor<T> c) {
			this.c = c;
		}

		@Override
		public T born(ResultSet rs) throws Exception {
			return c.newInstance(rs);
		}
	}

	private EntityName tableName;
	private EntityName viewName;
	private EntityField idField;
	private Map<String, Link> manys;
	private Map<String, Link> ones;
	private Map<String, Link> manyManys;
	private EntityField nameField;
	private Borning<T> borning;

	public EntityField getIdField() {
		return idField;
	}

	public EntityField getNameField() {
		return nameField;
	}

	public EntityField getIdentifiedField() {
		if (null != idField)
			return idField;
		return nameField;
	}

	public String getTableName() {
		return tableName.value();
	}

	public String getViewName() {
		return viewName.value();
	}

	/**
	 * Analyze one entity's setting. !!! This function must be invoked before
	 * another method.
	 * 
	 * @param classOfT
	 * @return TODO
	 */
	boolean parse(Class<T> classOfT) {
		this.mirror = Mirror.me(classOfT);
		Table table = classOfT.getAnnotation(Table.class);
		if (null == table)
			return false;
		// eval table name
		if (CONST.NULL.equals(table.value()))
			tableName = EntityName.create(mirror.getType().getSimpleName().toLowerCase());
		else
			tableName = EntityName.create(table.value());
		// eval view name
		View view = classOfT.getAnnotation(View.class);
		if (null == view) {
			viewName = tableName;
		} else {
			viewName = EntityName.create(view.value());
		}

		// evalu fields
		this.fieldMapping = new HashMap<String, EntityField>();
		evalBorning(this);

		/* parse all children fields */
		for (Field f : mirror.getFields()) {
			EntityField ef = new EntityField(this);
			Object re = ef.valueOf(mirror, f);
			if (re instanceof Boolean) {
				fieldMapping.put(f.getName(), ef);
				if (ef.isId() && ef.isName())
					throw new ErrorEntitySyntaxException(classOfT, String.format(
							"field '%s' can not be @Id and @Name at same time", f.getName()));
				if (ef.isId()) {
					if (null != idField)
						throw new ErrorEntitySyntaxException(classOfT, String.format(
								"->[%s] : duplicate ID Field with [%s]", f.getName(), idField
										.getField().getName()));
					if (Number.class.isAssignableFrom(f.getType())) {
						throw new ErrorEntitySyntaxException(classOfT, String.format(
								"->[%s] : ID field must be a number!", f.getName()));
					}
					idField = ef;
				}
				if (ef.isName()) {
					if (null != this.nameField)
						throw new ErrorEntitySyntaxException(classOfT, String.format(
								"->[%s] : duplicate Name Field with [%s]", f.getName(), nameField
										.getField().getName()));
					if (!CharSequence.class.isAssignableFrom(f.getType())) {
						throw new ErrorEntitySyntaxException(classOfT, String.format(
								"->[%s] : shall be a sub-class of %s", f.getName(),
								CharSequence.class.getName()));
					}
					nameField = ef;
				}
			} else if (re instanceof Link) {
				if (((Link) re).isMany())
					this.manys.put(((Link) re).getOwnField().getName(), ((Link) re));
				else if (((Link) re).isManyMany())
					this.manyManys.put(((Link) re).getOwnField().getName(), ((Link) re));
				else
					this.ones.put(((Link) re).getOwnField().getName(), ((Link) re));
			}
		}
		/* done for parse all children fields */
		return true;
	}

	private static <T> void evalBorning(Entity<T> entity) {
		Class<T> type = entity.mirror.getType();
		Method rsMethod = null;
		Method defMethod = null;
		for (Method method : entity.mirror.getStaticMethods()) {
			if (entity.mirror.is(method.getReturnType())) {
				Class<?>[] pts = method.getParameterTypes();
				if (pts.length == 0)
					defMethod = method;
				else if (pts.length == 1 && pts[0] == ResultSet.class)
					rsMethod = method;
			}
		}
		// static POJO getInstance(ResultSet);
		if (null != rsMethod) {
			entity.borning = new StaticResultSetMethodBorning<T>(rsMethod);
		} else {
			try { // new POJO(ResultSet)
				entity.borning = new ResultSetConstructorBorning<T>(type
						.getConstructor(ResultSet.class));
			} catch (Exception e) {
				// static POJO getInstance();
				if (null != defMethod)
					entity.borning = new DefaultStaticMethodBorning<T>(entity, defMethod);
				else
					try {
						// new POJO()
						entity.borning = new DefaultConstructorBorning<T>(entity, type
								.getConstructor());
					} catch (Exception e1) {
						throw Lang
								.makeThrow(
										"Entity [%s] is invailid, it should has at least one of:"
												+ " \n1. %s \n2.%s \n3. %s \n4. %s, \n(%s)",
										type.getName(),
										"Accessable constructor with one parameter type as java.sql.ResultSet",
										"Accessable static method with one parameter type as java.sql.ResultSet and return type is ["
												+ type.getName() + "]",
										"Accessable static method without parameter and return type is ["
												+ type.getName() + "]",
										"Accessable default constructor",
										"I will try to invoke those borning methods following the order above.");
					}
			}
		}
	}

	public Collection<EntityField> fields() {
		return fieldMapping.values();
	}

	public EntityField getField(String name) {
		return this.fieldMapping.get(name);
	}

	public Mirror<T> getMirror() {
		return mirror;
	}

	public T getObject(final ResultSet rs) {
		try {
			return borning.born(rs);
		} catch (Exception e) {
			throw Lang.wrapThrow(e);
		}
	}

	public Map<String, Link> getManys() {
		return manys;
	}

	public Map<String, Link> getOnes() {
		return ones;
	}

	public Map<String, Link> getManyManys() {
		return manyManys;
	}

}