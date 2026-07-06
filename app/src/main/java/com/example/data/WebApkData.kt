package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "web_apk_projects")
data class WebApkProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val packageName: String,
    val orientation: String, // "PORTRAIT", "LANDSCAPE", "UNSPECIFIED"
    val displayMode: String, // "FULLSCREEN", "EDGE_TO_EDGE", "STANDARD"
    val enableJs: Boolean,
    val enableZoom: Boolean,
    val domStorage: Boolean,
    val themeColor: String, // "EMERALD", "ROYAL_PURPLE", "OCEAN_BLUE", "CRIMSON", "AMBER"
    val appIcon: String = "language", // "language", "shopping_cart", "business", "article", "chat", "gamepad", "school", "music_note"
    val apkFileName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun resolveApkFileName(): String {
        return if (apkFileName.isNotBlank()) {
            var cleanName = apkFileName.trim().replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
            if (!cleanName.endsWith(".apk", ignoreCase = true)) {
                cleanName = "$cleanName.apk"
            }
            cleanName
        } else {
            val safeName = name.replace("[^a-zA-Z0-9]".toRegex(), "_")
            "${safeName}_LustWebApk.apk"
        }
    }
}

@Dao
interface WebApkDao {
    @Query("SELECT * FROM web_apk_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<WebApkProject>>

    @Query("SELECT * FROM web_apk_projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<WebApkProject?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: WebApkProject): Long

    @Delete
    suspend fun deleteProject(project: WebApkProject)
}

@Database(entities = [WebApkProject::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val dao: WebApkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "web_apk_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WebApkRepository(private val dao: WebApkDao) {
    val allProjects: Flow<List<WebApkProject>> = dao.getAllProjects()

    fun getProjectById(id: Long) = dao.getProjectById(id)

    suspend fun insert(project: WebApkProject): Long = dao.insertProject(project)

    suspend fun delete(project: WebApkProject) = dao.deleteProject(project)
}
