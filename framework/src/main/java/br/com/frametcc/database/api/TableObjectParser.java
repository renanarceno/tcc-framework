package br.com.frametcc.database.api;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.frametcc.TCCApplication;
import br.com.frametcc.database.annotation.Column;
import br.com.frametcc.database.api.adapter.NullTypeAdapter;
import br.com.frametcc.database.api.adapter.TypeAdapter;
import br.com.frametcc.database.helper.ReflectionHelper;

public class TableObjectParser<E> {

    private TypeAdapter typeAdapterInstance = TypeAdapter.getInstance();

    public ContentValues getInsertContentValues(E obj, Map<String, Field> columnsMap) {
        ContentValues values = new ContentValues();
        Object value;
        Field field;
        String columnName;

        for (Map.Entry<String, Field> entry : columnsMap.entrySet()) {
            columnName = entry.getKey();
            field = entry.getValue();
            value = ReflectionHelper.getValue(field, obj);
            typeAdapterInstance.setValue(values, value == null ? null : value.getClass(), columnName, value);

            /*for (Field valueField : value.getClass().getFields()) {
                Column annotation = valueField.getAnnotation(Column.class);
                if (annotation != null && (annotation.isPrimaryKey() || annotation.isAutoIncrementPrimaryKey()) && annotation.name().equalsIgnoreCase(keyRef)) {
                    typeAdapterInstance.setValue(values, valueField.getType(), columnName, value);
                    break;
                }
            }*/

        }
        return values;
    }

    public List<E> getObjectListFromCursor(Cursor cursor, DBListener<E> listener, Class<E> entity, Map<String, Field> columnsMap) {
        List<E> objs = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            objs.add(getObjectFromCursor(cursor, listener, entity, columnsMap));
            cursor.moveToNext();
        }
        return objs;
    }

    @SuppressWarnings("all")
    public E getObjectFromCursor(Cursor cursor, DBListener<E> listener, Class<E> entityClass, Map<String, Field> entityColumnsMap) {
        Field field;
        String columnName;
        E entity = (E) ReflectionHelper.getNewInstance(entityClass);
        Column column;
        int columnIndex;
        for (Map.Entry<String, Field> entry : entityColumnsMap.entrySet()) {
            columnName = entry.getKey();
            field = entry.getValue();
            columnIndex = cursor.getColumnIndex(columnName);
            String keyRef = field.getAnnotation(Column.class).foreignKeyRef();
            if (!keyRef.equals("")) {
                Object foreignObject = ReflectionHelper.getValue(field, entity);
                if (foreignObject == null) {
                    foreignObject = ReflectionHelper.getNewInstance(field.getType());
                    ReflectionHelper.setFieldValue(field, entity, foreignObject);
                }
                Class<?> foreignObjectClass = foreignObject.getClass();
                Map<String, Field> primaryKeyMap = TCCApplication.getDbConnection().getTableSpec(foreignObjectClass).getPrimaryKeyMap();
                Field foreignField = primaryKeyMap.get(keyRef);
                if (foreignField != null) {
                    if (this.typeAdapterInstance.getAdapter(foreignField.getType()) != null) {
                        Object value;
                        if (!cursor.isNull(columnIndex)) {
                            value = this.typeAdapterInstance.getValue(foreignField.getType(), columnIndex, cursor);
                        } else {
                            value = this.typeAdapterInstance.getValue(null, columnIndex, cursor);
                        }
                        if (listener != null)
                            listener.onRetrieve(entity);
                        ReflectionHelper.setFieldValue(foreignField, foreignObject, value);
                    }
                }
            } else {
                Object value = this.typeAdapterInstance.getValue(field.getType(), columnIndex, cursor);
                if (listener != null)
                    listener.onRetrieve(entity);
                ReflectionHelper.setFieldValue(field, entity, value);
            }
        }
        return entity;
    }
}