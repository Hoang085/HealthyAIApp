package com.healthyai.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.healthyai.app.data.local.Converters;
import com.healthyai.app.data.local.entity.HabitLogEntity;
import com.healthyai.app.domain.model.HabitCategory;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HabitLogDao_Impl implements HabitLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HabitLogEntity> __insertionAdapterOfHabitLogEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfMarkAsSynced;

  public HabitLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHabitLogEntity = new EntityInsertionAdapter<HabitLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habit_logs` (`id`,`habitId`,`habitName`,`category`,`value`,`unit`,`targetValueAtTime`,`loggedAt`,`note`,`isDirty`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitLogEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getHabitId());
        statement.bindString(3, entity.getHabitName());
        final String _tmp = __converters.fromHabitCategory(entity.getCategory());
        statement.bindString(4, _tmp);
        statement.bindDouble(5, entity.getValue());
        statement.bindString(6, entity.getUnit());
        statement.bindDouble(7, entity.getTargetValueAtTime());
        statement.bindLong(8, entity.getLoggedAt());
        statement.bindString(9, entity.getNote());
        final int _tmp_1 = entity.isDirty() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
      }
    };
    this.__preparedStmtOfMarkAsSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE habit_logs SET isDirty = 0 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertLog(final HabitLogEntity log, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHabitLogEntity.insertAndReturnId(log);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsSynced(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkAsSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HabitLogEntity>> getLogsFromTimestamp(final long fromTimestamp) {
    final String _sql = "SELECT * FROM habit_logs WHERE loggedAt >= ? ORDER BY loggedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fromTimestamp);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habit_logs"}, new Callable<List<HabitLogEntity>>() {
      @Override
      @NonNull
      public List<HabitLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfHabitName = CursorUtil.getColumnIndexOrThrow(_cursor, "habitName");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTargetValueAtTime = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValueAtTime");
          final int _cursorIndexOfLoggedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "loggedAt");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final List<HabitLogEntity> _result = new ArrayList<HabitLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHabitId;
            _tmpHabitId = _cursor.getLong(_cursorIndexOfHabitId);
            final String _tmpHabitName;
            _tmpHabitName = _cursor.getString(_cursorIndexOfHabitName);
            final HabitCategory _tmpCategory;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toHabitCategory(_tmp);
            final float _tmpValue;
            _tmpValue = _cursor.getFloat(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final float _tmpTargetValueAtTime;
            _tmpTargetValueAtTime = _cursor.getFloat(_cursorIndexOfTargetValueAtTime);
            final long _tmpLoggedAt;
            _tmpLoggedAt = _cursor.getLong(_cursorIndexOfLoggedAt);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final boolean _tmpIsDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp_1 != 0;
            _item = new HabitLogEntity(_tmpId,_tmpHabitId,_tmpHabitName,_tmpCategory,_tmpValue,_tmpUnit,_tmpTargetValueAtTime,_tmpLoggedAt,_tmpNote,_tmpIsDirty);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getUnsyncedLogs(final Continuation<? super List<HabitLogEntity>> $completion) {
    final String _sql = "SELECT * FROM habit_logs WHERE isDirty = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HabitLogEntity>>() {
      @Override
      @NonNull
      public List<HabitLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfHabitName = CursorUtil.getColumnIndexOrThrow(_cursor, "habitName");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTargetValueAtTime = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValueAtTime");
          final int _cursorIndexOfLoggedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "loggedAt");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final List<HabitLogEntity> _result = new ArrayList<HabitLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHabitId;
            _tmpHabitId = _cursor.getLong(_cursorIndexOfHabitId);
            final String _tmpHabitName;
            _tmpHabitName = _cursor.getString(_cursorIndexOfHabitName);
            final HabitCategory _tmpCategory;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toHabitCategory(_tmp);
            final float _tmpValue;
            _tmpValue = _cursor.getFloat(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final float _tmpTargetValueAtTime;
            _tmpTargetValueAtTime = _cursor.getFloat(_cursorIndexOfTargetValueAtTime);
            final long _tmpLoggedAt;
            _tmpLoggedAt = _cursor.getLong(_cursorIndexOfLoggedAt);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final boolean _tmpIsDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp_1 != 0;
            _item = new HabitLogEntity(_tmpId,_tmpHabitId,_tmpHabitName,_tmpCategory,_tmpValue,_tmpUnit,_tmpTargetValueAtTime,_tmpLoggedAt,_tmpNote,_tmpIsDirty);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecentLogsByCategory(final String category, final int limit,
      final Continuation<? super List<HabitLogEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM habit_logs \n"
            + "        WHERE category = ? \n"
            + "        ORDER BY loggedAt DESC \n"
            + "        LIMIT ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HabitLogEntity>>() {
      @Override
      @NonNull
      public List<HabitLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfHabitName = CursorUtil.getColumnIndexOrThrow(_cursor, "habitName");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTargetValueAtTime = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValueAtTime");
          final int _cursorIndexOfLoggedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "loggedAt");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfIsDirty = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirty");
          final List<HabitLogEntity> _result = new ArrayList<HabitLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHabitId;
            _tmpHabitId = _cursor.getLong(_cursorIndexOfHabitId);
            final String _tmpHabitName;
            _tmpHabitName = _cursor.getString(_cursorIndexOfHabitName);
            final HabitCategory _tmpCategory;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toHabitCategory(_tmp);
            final float _tmpValue;
            _tmpValue = _cursor.getFloat(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final float _tmpTargetValueAtTime;
            _tmpTargetValueAtTime = _cursor.getFloat(_cursorIndexOfTargetValueAtTime);
            final long _tmpLoggedAt;
            _tmpLoggedAt = _cursor.getLong(_cursorIndexOfLoggedAt);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final boolean _tmpIsDirty;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDirty);
            _tmpIsDirty = _tmp_1 != 0;
            _item = new HabitLogEntity(_tmpId,_tmpHabitId,_tmpHabitName,_tmpCategory,_tmpValue,_tmpUnit,_tmpTargetValueAtTime,_tmpLoggedAt,_tmpNote,_tmpIsDirty);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
