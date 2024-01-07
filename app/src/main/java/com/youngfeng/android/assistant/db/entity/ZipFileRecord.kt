package com.youngfeng.android.assistant.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "t_zip_file_record")
data class ZipFileRecord(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String,
    var path: String,
    var md5: String,
    @ColumnInfo(name = "original_paths_md5")
    var originalPathsMD5: String,
    @ColumnInfo(name = "original_files_md5")
    var originalFilesMD5: String,
    @ColumnInfo(name = "is_multi_original_file")
    var isMultiOriginalFile: Boolean,
    @ColumnInfo(name = "create_time")
    var createTime: Long,
)
