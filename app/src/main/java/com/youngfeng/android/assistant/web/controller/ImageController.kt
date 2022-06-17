package com.youngfeng.android.assistant.web.controller

import android.media.MediaScannerConnection
import android.text.TextUtils
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RequestMapping
import com.yanzhenjie.andserver.annotation.ResponseBody
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.http.HttpRequest
import com.youngfeng.android.assistant.R
import com.youngfeng.android.assistant.app.MobileAssistantApplication
import com.youngfeng.android.assistant.ext.getString
import com.youngfeng.android.assistant.util.PhotoUtil
import com.youngfeng.android.assistant.web.HttpError
import com.youngfeng.android.assistant.web.HttpModule
import com.youngfeng.android.assistant.web.entity.*
import com.youngfeng.android.assistant.web.request.DeleteAlbumsRequest
import com.youngfeng.android.assistant.web.request.DeleteImageRequest
import com.youngfeng.android.assistant.web.request.GetAlbumImagesRequest
import com.youngfeng.android.assistant.web.util.ErrorBuilder
import timber.log.Timber
import java.io.File
import java.lang.Exception
import java.util.Locale

@RestController
@RequestMapping("/image")
class ImageController {
    private val mContext by lazy { MobileAssistantApplication.getInstance() }

    @PostMapping("/albums")
    @ResponseBody
    fun getAlbums(): HttpResponseEntity<List<AlbumEntity>> {
        val albums = PhotoUtil.getAllAlbums(mContext)
        return HttpResponseEntity.success(albums)
    }

    @PostMapping("/all")
    @ResponseBody
    fun getAllImages(): HttpResponseEntity<List<ImageEntity>> {
        val images = PhotoUtil.getAllImages(mContext)
        return HttpResponseEntity.success(images)
    }

    @PostMapping("/daily")
    fun getDailyImages(): HttpResponseEntity<List<DailyImageEntity>> {
        throw NotImplementedError()
    }

    @PostMapping("/monthly")
    fun getMonthlyImages(): HttpResponseEntity<List<MonthlyImageEntity>> {
        throw NotImplementedError()
    }

    @PostMapping("/albumImages")
    @ResponseBody
    fun getAlbumImages(): HttpResponseEntity<List<ImageEntity>> {
        val images = PhotoUtil.getAlbumImages(mContext)
        return HttpResponseEntity.success(images)
    }

    @PostMapping("/delete")
    @ResponseBody
    fun deleteImage(httpRequest: HttpRequest, @RequestBody request: DeleteImageRequest): HttpResponseEntity<Any> {
        val languageCode = httpRequest.getHeader("languageCode")
        val locale = if (!TextUtils.isEmpty(languageCode)) Locale(languageCode!!) else Locale("en")

        try {
            val resultMap = HashMap<String, String>()
            val imageFiles = ArrayList<String>()
            var isAllSuccess = true
            request.paths.forEach { imgPath ->
                val imageFile = File(imgPath)
                imageFiles.add(imageFile.absolutePath)
                if (!imageFile.exists()) {
                    isAllSuccess = false
                    resultMap[imgPath] = mContext.getString(locale, HttpError.ImageFileNotExist.value)
                } else {
                    val isSuccess = imageFile.delete()
                    if (!isSuccess) {
                        isAllSuccess = false
                        resultMap[imgPath] = mContext.getString(locale, HttpError.DeleteImageFail.value)
                    }
                }
            }
            if (imageFiles.size > 0) {
                MediaScannerConnection.scanFile(mContext, imageFiles.toTypedArray(), null) { path, uri ->
                    Timber.d("Path: $path, uri: ${uri?.path}")
                }
            }
            if (!isAllSuccess) {
                val response = ErrorBuilder().locale(locale).module(HttpModule.ImageModule).error(HttpError.DeleteImageFail).build<Any>()
                response.msg = resultMap.map { "${it.key}[${it.value}];" }.toString()
                return response
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val response = ErrorBuilder().locale(locale).module(HttpModule.ImageModule).error(HttpError.DeleteImageFail).build<Any>()
            response.msg = e.message
            return response
        }

        return HttpResponseEntity.success()
    }

    @PostMapping("/deleteAlbums")
    @ResponseBody
    fun deleteAlbums(httpRequest: HttpRequest, @RequestBody request: DeleteAlbumsRequest): HttpResponseEntity<Any> {
        val languageCode = httpRequest.getHeader("languageCode")
        val locale = if (!TextUtils.isEmpty(languageCode)) Locale(languageCode!!) else Locale("en")
        try {
            val paths = request.paths

            var deleteItemNum = 0
            paths.forEach { path ->
                val file = File(path)
                if (!file.exists()) {
                    val response = ErrorBuilder().locale(locale).module(HttpModule.ImageModule).error(HttpError.DeleteAlbumFail).build<Any>()
                    response.msg = convertToDeleteAlbumError(locale, paths.size, deleteItemNum)
                    return response
                } else {
                    val isSuccess = file.deleteRecursively()
                    if (!isSuccess) {
                        val response = ErrorBuilder().locale(locale).module(HttpModule.ImageModule).error(HttpError.DeleteAlbumFail).build<Any>()
                        response.msg = convertToDeleteAlbumError(locale, paths.size, deleteItemNum)
                        return response
                    } else {
                        MediaScannerConnection.scanFile(mContext, arrayOf(path), null, null)
                    }
                }

                deleteItemNum ++
            }

            return HttpResponseEntity.success()
        } catch (e: Exception) {
            e.printStackTrace()
            val response = ErrorBuilder().locale(locale).module(HttpModule.ImageModule).error(HttpError.DeleteAlbumFail).build<Any>()
            response.msg = e.message
            return response
        }
    }

    private fun convertToDeleteAlbumError(locale: Locale, albumNum: Int, deletedItemNum: Int): String {
        if (deletedItemNum > 0) {
            return mContext.getString(locale, R.string.place_holder_delete_part_of_success)
                .format(albumNum, deletedItemNum)
        }

        return mContext.getString(locale, R.string.delete_album_fail)
    }

    @PostMapping("/imagesOfAlbum")
    @ResponseBody
    fun getImagesOfAlbum(@RequestBody request: GetAlbumImagesRequest): HttpResponseEntity<List<ImageEntity>> {
        val images = PhotoUtil.getImagesOfAlbum(mContext, request.id)
        return HttpResponseEntity.success(images)
    }
}
