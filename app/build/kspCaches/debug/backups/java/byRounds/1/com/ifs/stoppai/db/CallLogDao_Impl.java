package com.ifs.stoppai.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class CallLogDao_Impl implements CallLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CallLogEntry> __insertionAdapterOfCallLogEntry;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateNota;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDisplayName;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSmsRisposta;

  public CallLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCallLogEntry = new EntityInsertionAdapter<CallLogEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `call_log_entries` (`id`,`phoneNumber`,`callType`,`timestamp`,`statusId`,`nota`,`ariaNote`,`callOutcome`,`callDirection`,`displayName`,`smsInviato`,`smsRisposta`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CallLogEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getPhoneNumber());
        statement.bindString(3, entity.getCallType());
        statement.bindLong(4, entity.getTimestamp());
        statement.bindLong(5, entity.getStatusId());
        statement.bindString(6, entity.getNota());
        statement.bindString(7, entity.getAriaNote());
        statement.bindString(8, entity.getCallOutcome());
        statement.bindString(9, entity.getCallDirection());
        statement.bindString(10, entity.getDisplayName());
        final int _tmp = entity.getSmsInviato() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getSmsRisposta() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getSmsRisposta());
        }
      }
    };
    this.__preparedStmtOfUpdateStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE call_log_entries SET statusId = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateNota = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE call_log_entries SET nota = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM call_log_entries WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateDisplayName = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE call_log_entries SET displayName = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSmsRisposta = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE call_log_entries SET smsRisposta = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertCallLog(final CallLogEntry callLog,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCallLogEntry.insert(callLog);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStatus(final long id, final int status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatus.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, status);
        _argIndex = 2;
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
          __preparedStmtOfUpdateStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNota(final long id, final String nota,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateNota.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, nota);
        _argIndex = 2;
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
          __preparedStmtOfUpdateNota.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDisplayName(final long id, final String name,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDisplayName.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, name);
        _argIndex = 2;
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
          __preparedStmtOfUpdateDisplayName.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSmsRisposta(final long id, final String text,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSmsRisposta.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, text);
        _argIndex = 2;
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
          __preparedStmtOfUpdateSmsRisposta.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CallLogEntry>> getAllCalls() {
    final String _sql = "SELECT * FROM call_log_entries ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"call_log_entries"}, new Callable<List<CallLogEntry>>() {
      @Override
      @NonNull
      public List<CallLogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatusId = CursorUtil.getColumnIndexOrThrow(_cursor, "statusId");
          final int _cursorIndexOfNota = CursorUtil.getColumnIndexOrThrow(_cursor, "nota");
          final int _cursorIndexOfAriaNote = CursorUtil.getColumnIndexOrThrow(_cursor, "ariaNote");
          final int _cursorIndexOfCallOutcome = CursorUtil.getColumnIndexOrThrow(_cursor, "callOutcome");
          final int _cursorIndexOfCallDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "callDirection");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfSmsInviato = CursorUtil.getColumnIndexOrThrow(_cursor, "smsInviato");
          final int _cursorIndexOfSmsRisposta = CursorUtil.getColumnIndexOrThrow(_cursor, "smsRisposta");
          final List<CallLogEntry> _result = new ArrayList<CallLogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpCallType;
            _tmpCallType = _cursor.getString(_cursorIndexOfCallType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpStatusId;
            _tmpStatusId = _cursor.getInt(_cursorIndexOfStatusId);
            final String _tmpNota;
            _tmpNota = _cursor.getString(_cursorIndexOfNota);
            final String _tmpAriaNote;
            _tmpAriaNote = _cursor.getString(_cursorIndexOfAriaNote);
            final String _tmpCallOutcome;
            _tmpCallOutcome = _cursor.getString(_cursorIndexOfCallOutcome);
            final String _tmpCallDirection;
            _tmpCallDirection = _cursor.getString(_cursorIndexOfCallDirection);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpSmsInviato;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSmsInviato);
            _tmpSmsInviato = _tmp != 0;
            final String _tmpSmsRisposta;
            if (_cursor.isNull(_cursorIndexOfSmsRisposta)) {
              _tmpSmsRisposta = null;
            } else {
              _tmpSmsRisposta = _cursor.getString(_cursorIndexOfSmsRisposta);
            }
            _item = new CallLogEntry(_tmpId,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpStatusId,_tmpNota,_tmpAriaNote,_tmpCallOutcome,_tmpCallDirection,_tmpDisplayName,_tmpSmsInviato,_tmpSmsRisposta);
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
  public int getCallsToday(final long startOfDay) {
    final String _sql = "SELECT COUNT(*) FROM call_log_entries WHERE timestamp > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getTotalCalls() {
    final String _sql = "SELECT COUNT(*) FROM call_log_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Object getCountForNumber(final String number,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM call_log_entries WHERE phoneNumber = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, number);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object isNumberReliable(final String number,
      final Continuation<? super CallLogEntry> $completion) {
    final String _sql = "SELECT * FROM call_log_entries WHERE phoneNumber = ? AND statusId = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, number);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CallLogEntry>() {
      @Override
      @Nullable
      public CallLogEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatusId = CursorUtil.getColumnIndexOrThrow(_cursor, "statusId");
          final int _cursorIndexOfNota = CursorUtil.getColumnIndexOrThrow(_cursor, "nota");
          final int _cursorIndexOfAriaNote = CursorUtil.getColumnIndexOrThrow(_cursor, "ariaNote");
          final int _cursorIndexOfCallOutcome = CursorUtil.getColumnIndexOrThrow(_cursor, "callOutcome");
          final int _cursorIndexOfCallDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "callDirection");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfSmsInviato = CursorUtil.getColumnIndexOrThrow(_cursor, "smsInviato");
          final int _cursorIndexOfSmsRisposta = CursorUtil.getColumnIndexOrThrow(_cursor, "smsRisposta");
          final CallLogEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpCallType;
            _tmpCallType = _cursor.getString(_cursorIndexOfCallType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpStatusId;
            _tmpStatusId = _cursor.getInt(_cursorIndexOfStatusId);
            final String _tmpNota;
            _tmpNota = _cursor.getString(_cursorIndexOfNota);
            final String _tmpAriaNote;
            _tmpAriaNote = _cursor.getString(_cursorIndexOfAriaNote);
            final String _tmpCallOutcome;
            _tmpCallOutcome = _cursor.getString(_cursorIndexOfCallOutcome);
            final String _tmpCallDirection;
            _tmpCallDirection = _cursor.getString(_cursorIndexOfCallDirection);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpSmsInviato;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSmsInviato);
            _tmpSmsInviato = _tmp != 0;
            final String _tmpSmsRisposta;
            if (_cursor.isNull(_cursorIndexOfSmsRisposta)) {
              _tmpSmsRisposta = null;
            } else {
              _tmpSmsRisposta = _cursor.getString(_cursorIndexOfSmsRisposta);
            }
            _result = new CallLogEntry(_tmpId,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpStatusId,_tmpNota,_tmpAriaNote,_tmpCallOutcome,_tmpCallDirection,_tmpDisplayName,_tmpSmsInviato,_tmpSmsRisposta);
          } else {
            _result = null;
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
  public Object getAllCallsSync(final Continuation<? super List<CallLogEntry>> $completion) {
    final String _sql = "SELECT * FROM call_log_entries ORDER BY timestamp DESC LIMIT 100";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CallLogEntry>>() {
      @Override
      @NonNull
      public List<CallLogEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatusId = CursorUtil.getColumnIndexOrThrow(_cursor, "statusId");
          final int _cursorIndexOfNota = CursorUtil.getColumnIndexOrThrow(_cursor, "nota");
          final int _cursorIndexOfAriaNote = CursorUtil.getColumnIndexOrThrow(_cursor, "ariaNote");
          final int _cursorIndexOfCallOutcome = CursorUtil.getColumnIndexOrThrow(_cursor, "callOutcome");
          final int _cursorIndexOfCallDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "callDirection");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfSmsInviato = CursorUtil.getColumnIndexOrThrow(_cursor, "smsInviato");
          final int _cursorIndexOfSmsRisposta = CursorUtil.getColumnIndexOrThrow(_cursor, "smsRisposta");
          final List<CallLogEntry> _result = new ArrayList<CallLogEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CallLogEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpCallType;
            _tmpCallType = _cursor.getString(_cursorIndexOfCallType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpStatusId;
            _tmpStatusId = _cursor.getInt(_cursorIndexOfStatusId);
            final String _tmpNota;
            _tmpNota = _cursor.getString(_cursorIndexOfNota);
            final String _tmpAriaNote;
            _tmpAriaNote = _cursor.getString(_cursorIndexOfAriaNote);
            final String _tmpCallOutcome;
            _tmpCallOutcome = _cursor.getString(_cursorIndexOfCallOutcome);
            final String _tmpCallDirection;
            _tmpCallDirection = _cursor.getString(_cursorIndexOfCallDirection);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpSmsInviato;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSmsInviato);
            _tmpSmsInviato = _tmp != 0;
            final String _tmpSmsRisposta;
            if (_cursor.isNull(_cursorIndexOfSmsRisposta)) {
              _tmpSmsRisposta = null;
            } else {
              _tmpSmsRisposta = _cursor.getString(_cursorIndexOfSmsRisposta);
            }
            _item = new CallLogEntry(_tmpId,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpStatusId,_tmpNota,_tmpAriaNote,_tmpCallOutcome,_tmpCallDirection,_tmpDisplayName,_tmpSmsInviato,_tmpSmsRisposta);
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
  public Object getLastEntryForNumber(final String number,
      final Continuation<? super CallLogEntry> $completion) {
    final String _sql = "SELECT * FROM call_log_entries WHERE phoneNumber = ? ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, number);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CallLogEntry>() {
      @Override
      @Nullable
      public CallLogEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfCallType = CursorUtil.getColumnIndexOrThrow(_cursor, "callType");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatusId = CursorUtil.getColumnIndexOrThrow(_cursor, "statusId");
          final int _cursorIndexOfNota = CursorUtil.getColumnIndexOrThrow(_cursor, "nota");
          final int _cursorIndexOfAriaNote = CursorUtil.getColumnIndexOrThrow(_cursor, "ariaNote");
          final int _cursorIndexOfCallOutcome = CursorUtil.getColumnIndexOrThrow(_cursor, "callOutcome");
          final int _cursorIndexOfCallDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "callDirection");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfSmsInviato = CursorUtil.getColumnIndexOrThrow(_cursor, "smsInviato");
          final int _cursorIndexOfSmsRisposta = CursorUtil.getColumnIndexOrThrow(_cursor, "smsRisposta");
          final CallLogEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpCallType;
            _tmpCallType = _cursor.getString(_cursorIndexOfCallType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpStatusId;
            _tmpStatusId = _cursor.getInt(_cursorIndexOfStatusId);
            final String _tmpNota;
            _tmpNota = _cursor.getString(_cursorIndexOfNota);
            final String _tmpAriaNote;
            _tmpAriaNote = _cursor.getString(_cursorIndexOfAriaNote);
            final String _tmpCallOutcome;
            _tmpCallOutcome = _cursor.getString(_cursorIndexOfCallOutcome);
            final String _tmpCallDirection;
            _tmpCallDirection = _cursor.getString(_cursorIndexOfCallDirection);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpSmsInviato;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSmsInviato);
            _tmpSmsInviato = _tmp != 0;
            final String _tmpSmsRisposta;
            if (_cursor.isNull(_cursorIndexOfSmsRisposta)) {
              _tmpSmsRisposta = null;
            } else {
              _tmpSmsRisposta = _cursor.getString(_cursorIndexOfSmsRisposta);
            }
            _result = new CallLogEntry(_tmpId,_tmpPhoneNumber,_tmpCallType,_tmpTimestamp,_tmpStatusId,_tmpNota,_tmpAriaNote,_tmpCallOutcome,_tmpCallDirection,_tmpDisplayName,_tmpSmsInviato,_tmpSmsRisposta);
          } else {
            _result = null;
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
