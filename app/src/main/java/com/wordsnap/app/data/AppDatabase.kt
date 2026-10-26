package com.wordsnap.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Word::class, Profile::class, Book::class, Item::class], version = 11)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun profileDao(): ProfileDao
    abstract fun bookDao(): BookDao
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wordsnap.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build().also { instance = it }
            }

        // v0.3 加入複習欄位，保留舊的單字
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN box INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE words ADD COLUMN dueAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v0.4 加入暱稱：舊的單字歸給一個叫「我」的暱稱
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE words ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "INSERT INTO profiles (id, name, createdAt) " +
                        "SELECT 1, '我', ${System.currentTimeMillis()} WHERE EXISTS (SELECT 1 FROM words)"
                )
            }
        }

        // v0.5 加入單字簿和像素人物：每個暱稱建立一本「我的單字簿」，舊單字都放進去
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `books` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE words ADD COLUMN bookId INTEGER")
                db.execSQL("ALTER TABLE profiles ADD COLUMN avatar TEXT")
                db.execSQL(
                    "INSERT INTO books (profileId, name, createdAt) " +
                        "SELECT id, '$DEFAULT_BOOK', ${System.currentTimeMillis()} FROM profiles"
                )
                db.execSQL(
                    "UPDATE words SET bookId = (SELECT b.id FROM books b WHERE b.profileId = words.profileId)"
                )
            }
        }

        // v0.6 加入戰鬥模式和人物等級
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN testedAt INTEGER")
                db.execSQL("ALTER TABLE profiles ADD COLUMN exp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE books ADD COLUMN clearedAt INTEGER")
            }
        }

        // v0.65 記錄戰鬥模式答錯次數
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN wrongCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v0.68 記錄被打倒的次數
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN deaths INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v0.69 預設的單字簿改名為「我的冒險日誌」
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE books SET name = '$DEFAULT_BOOK' WHERE name = '我的單字簿'")
            }
        }

        // v0.70 記錄打倒了幾隻中魔王、大魔王
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN midBossSeen INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profiles ADD COLUMN bigBossSeen INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v0.71 加入道具袋，並記錄每個字在戰鬥中考過幾次
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `items` (`profileId` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                        "`count` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `type`))"
                )
                db.execSQL("ALTER TABLE words ADD COLUMN testCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v0.94 刪掉單字簿時，裡面記住的字數記在冒險者身上
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN keptRemembered INTEGER NOT NULL DEFAULT 0")
            }
        }

        const val DEFAULT_BOOK = "我的冒險日誌"
    }
}
