package com.ours.yours.app.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orhanobut.logger.Logger;
import com.ours.yours.app.entity.Card;

import java.util.List;

public class CardsDao  {
    private final static int FAIL = -1;
    private final static int FAIL_NULL_DB_OBJECT = -2;
    private final static int SUCCEED = 0;
    private ContentResolver mContentResolver;
    private static CardsDao mInstance;

    public CardsDao(Context context) {
        Logger.d(">>>");
        mContentResolver = context.getContentResolver();
    }

    /**
     * gets a singleton dao object managing cards databases
     */
    public static CardsDao getInstance(Context context) {
        if (mInstance == null) {
            Logger.d("... going to create instance");
            mInstance = new CardsDao(context);
        }
        return mInstance;
    }

    /**
     * adds a card to database of cards and returns uri of new row which is just inserted
     */
    public Uri add(Card card) throws IllegalAccessException {
        Logger.d(">>>");
        Uri new_card_uri;
        ContentValues values = new ContentValues();
        if (mContentResolver == null) {
            Logger.e("!!! content resolver is null");
            return null;
        }
        if (getContentValuesFromCard(card, values) != SUCCEED) {
            Logger.e("!!! get content values failed");
            return null;
        }
        new_card_uri = mContentResolver.insert(CardsProvider.CONTENT_PROVIDER_URI, values);
        return new_card_uri;
    }

    /**
     * removes cards at farther distance from database of cards and returns number of row which are affected
     */
    public int remove(int distance) throws ClassNotFoundException, NoSuchFieldException {
        Logger.d(">>>");
        int num_affected = 0;
        if (mContentResolver == null) {
            Logger.e("!!! content resolver is null");
            return num_affected;
        }
        num_affected = mContentResolver.delete(CardsProvider.CONTENT_PROVIDER_URI, "Distance" + ">?", new String[]{String.valueOf(distance)});
        return num_affected;
    }

    /**
     * synchronizes local database of cards with remote server and returns number of row which are affected
     * the parameter "cards" comes from remote server
     */
    public int sync(List<Card> cards) throws IllegalAccessException {
        Logger.d(">>>");
        int num_affected = 0;
        int num_affected_once;
        ContentValues values = new ContentValues();
        String profile_name;
        if (mContentResolver == null) {
            Logger.e("!!! content resolver is null");
            return num_affected;
        }
        for (Card card : cards) {
            if (getContentValuesFromCard(card, values) != SUCCEED) {
                Logger.e("!!! get content values failed");
                return num_affected;
            }
            profile_name = card.getProfileName();
            num_affected_once = mContentResolver.update(CardsProvider.CONTENT_PROVIDER_URI, values, "ProfileName=?", new String[] {profile_name});
            Logger.d("... updated for " + profile_name + " and number of updated row is " + num_affected_once);
            num_affected += num_affected_once;
        }
        return num_affected;
    }

    /**
     * finds cards from database which are in range up to the wanted distance
     * passing 0 will return all cards
     */
    public int find(int wanted_distance, List<Card> cards) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Logger.d(">>>");
        Cursor cursor;
        int ret;
        if (mContentResolver == null) {
            Logger.e("!!! content resolver is null");
            return FAIL_NULL_DB_OBJECT;
        }
        if (wanted_distance == 0) {
            cursor = mContentResolver.query(CardsProvider.CONTENT_PROVIDER_URI, null, null, null, null);
        } else {
            cursor = mContentResolver.query(CardsProvider.CONTENT_PROVIDER_URI, null, "Distance" + "<?", new String[] {String.valueOf(wanted_distance)}, null);
        }
        if (cursor == null) {
            Logger.e("!!! cursor is null");
            return FAIL;
        }
        Logger.d("... found " + cursor.getCount() + " cards within " + wanted_distance);
        ret = getCardsFromCursor(cursor, cards);
        cursor.close();
        return ret;
    }

    /**
     * returns number of all rows stored in local database
     */
    public int length(){
        Logger.d(">>>");
        Cursor cursor;
        int count = 0;
        if (mContentResolver == null) {
            Logger.e("!!! content resolver is null");
            return count;
        }
        cursor = mContentResolver.query(CardsProvider.CONTENT_PROVIDER_URI, null, null, null, null);
        if (cursor == null) {
            Logger.e("!!! cursor is null");
            return count;
        }
        count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * marshals a card to a content value
     */
    private int getContentValuesFromCard(Card card, ContentValues values) throws IllegalAccessException {
        Logger.d(">>>");
        int ret;
        if (SQLDao.getInstance() == null) {
            Logger.e("!!! sql dao is null");
            return FAIL_NULL_DB_OBJECT;
        }
        ret = SQLDao.getInstance().parseModelToContentValues(card, values);
        return ret;
    }

    /**
     * de-marshals a card to a content value
     */
    private int getCardsFromCursor(Cursor cursor, List<Card> cards) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Logger.d(">>>");
        int ret;
        if (SQLDao.getInstance() == null) {
            Logger.e("!!! sql dao is null");
            return FAIL_NULL_DB_OBJECT;
        }
        ret = SQLDao.getInstance().parseCursorToModels(cursor, cards, Class.forName("com.ours.yours.app.entity.Card"));
        return ret;
    }
}
