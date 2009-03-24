package com.zzh.mvc.entity;

import com.zzh.lang.Lang;
import com.zzh.lang.Strings;
import com.zzh.mvc.Action;
import com.zzh.mvc.Return;
import com.zzh.service.EntityService;

public abstract class EntityAction<T> extends Action {

	protected EntityAction() {
		this.paramFields = Lang.array("id", "name");
	}

	protected EntityAction(EntityService<T> service) {
		this();
		this.service = service;

	}

	protected EntityService<T> service;

	public long id;

	public String name;

	@Override
	public Object execute() {
		try {
			if (!Strings.isEmpty(name))
				return execute(name);
			else
				return execute(id);
		} catch (Throwable e) {
			return Return.fail("Fail to %s '%s' by '%s', for the reason: %s", this.getClass()
					.getSimpleName(), service.getEntityClass().getName(),
					(Strings.isEmpty(name) ? id : name), e.getMessage());
		}
	}

	protected abstract Object execute(long id);

	protected abstract Object execute(String name);

}