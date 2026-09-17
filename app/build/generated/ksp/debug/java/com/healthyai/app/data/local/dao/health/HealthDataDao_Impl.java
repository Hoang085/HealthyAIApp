package com.healthyai.app.data.local.dao.health;

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
import com.healthyai.app.data.local.entity.health.HealthDataEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class HealthDataDao_Impl implements HealthDataDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HealthDataEntity> __insertionAdapterOfHealthDataEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldData;

  public HealthDataDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHealthDataEntity = new EntityInsertionAdapter<HealthDataEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `health_data_history` (`id`,`steps`,`sleepHours`,`heartRate`,`activeCalories`,`recordedAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthDataEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSteps());
        statement.bindDouble(3, entity.getSleepHours());
        statement.bindDouble(4, entity.getHeartRate());
        statement.bindDouble(5, entity.getActiveCalories());
        statement.bindLong(6, entity.getRecordedAt());
      }
    };
    this.__preparedStmtOfDeleteOldData = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM health_data_history WHERE recordedAt < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertHealthData(final HealthDataEntity data,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHealthDataEntity.insert(data);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldData(final long timestamp, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldData.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
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
          __preparedStmtOfDeleteOldData.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HealthDataEntity>> getRecentHealthData() {
    final String _sql = "SELECT * FROM health_data_history ORDER BY recordedAt DESC LIMIT 30";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_data_history"}, new Callable<List<HealthDataEntity>>() {
      @Override
      @NonNull
      public List<HealthDataEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfSleepHours = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepHours");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfActiveCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "activeCalories");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recordedAt");
          final List<HealthDataEntity> _result = new ArrayList<HealthDataEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HealthDataEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSteps;
            _tmpSteps = _cursor.getLong(_cursorIndexOfSteps);
            final double _tmpSleepHours;
            _tmpSleepHours = _cursor.getDouble(_cursorIndexOfSleepHours);
            final double _tmpHeartRate;
            _tmpHeartRate = _cursor.getDouble(_cursorIndexOfHeartRate);
            final double _tmpActiveCalories;
            _tmpActiveCalories = _cursor.getDouble(_cursorIndexOfActiveCalories);
            final long _tmpRecordedAt;
            _tmpRecordedAt = _cursor.getLong(_cursorIndexOfRecordedAt);
            _item = new HealthDataEntity(_tmpId,_tmpSteps,_tmpSleepHours,_tmpHeartRate,_tmpActiveCalories,_tmpRecordedAt);
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
  public Object getRecentHealthDataList(final int limit,
      final Continuation<? super List<HealthDataEntity>> $completion) {
    final String _sql = "SELECT * FROM health_data_history ORDER BY recordedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HealthDataEntity>>() {
      @Override
      @NonNull
      public List<HealthDataEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfSleepHours = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepHours");
          final int _cursorIndexOfHeartRate = CursorUtil.getColumnIndexOrThrow(_cursor, "heartRate");
          final int _cursorIndexOfActiveCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "activeCalories");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recordedAt");
          final List<HealthDataEntity> _result = new ArrayList<HealthDataEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HealthDataEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpSteps;
            _tmpSteps = _cursor.getLong(_cursorIndexOfSteps);
            final double _tmpSleepHours;
            _tmpSleepHours = _cursor.getDouble(_cursorIndexOfSleepHours);
            final double _tmpHeartRate;
            _tmpHeartRate = _cursor.getDouble(_cursorIndexOfHeartRate);
            final double _tmpActiveCalories;
            _tmpActiveCalories = _cursor.getDouble(_cursorIndexOfActiveCalories);
            final long _tmpRecordedAt;
            _tmpRecordedAt = _cursor.getLong(_cursorIndexOfRecordedAt);
            _item = new HealthDataEntity(_tmpId,_tmpSteps,_tmpSleepHours,_tmpHeartRate,_tmpActiveCalories,_tmpRecordedAt);
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
