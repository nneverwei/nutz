package com.zzh.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zzh.dao.callback.QueryCallback;

public class FetchSql<T> extends ConditionSql<T> {

	public FetchSql() {
		super();
	}

	public FetchSql(String sql) {
		super(sql);
	}

	private QueryCallback<T> callback;

	public FetchSql<T> setCallback(QueryCallback<T> callback) {
		this.callback = callback;
		return this;
	}

	public QueryCallback<T> getCallback() {
		return callback;
	}

	private FieldMatcher matcher;

	void setMatcher(FieldMatcher fm) {
		this.matcher = fm;
	}

	@Override
	public T execute(Connection conn) throws Exception {
		setResult(execute(conn, callback));
		return getResult();
	}

	protected T execute(Connection conn, QueryCallback<T> callback) throws SQLException {
		PreparedStatement stat = null;
		try {
			T o = null;
			stat = conn.prepareStatement(getPreparedStatementString(),
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			super.setupStatement(stat);
			ResultSet rs = stat.executeQuery();
			if (rs.first()) {
				callback.setMatcher(matcher);
				o = callback.invoke(rs);
			}
			rs.close();
			return o;
		} catch (Exception e) {
			throw new DaoException(this, e);
		} finally {
			if (null != stat)
				try {
					stat.close();
				} catch (SQLException e1) {}
		}
	}

}
