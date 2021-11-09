package com.youngfeng.android.assistant.web.converter

import com.google.gson.Gson
import com.yanzhenjie.andserver.annotation.Converter
import com.yanzhenjie.andserver.framework.MessageConverter
import com.yanzhenjie.andserver.framework.body.FileBody
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.IOUtils
import com.yanzhenjie.andserver.util.MediaType
import com.youngfeng.android.assistant.web.entity.HttpResponseEntity
import com.youngfeng.android.assistant.web.response.HttpResponseEntityBody
import com.youngfeng.android.assistant.web.util.JsonUtils
import java.io.InputStream
import java.lang.reflect.Type
import java.nio.charset.Charset

@Converter
class AppMessageConverter : MessageConverter {
    private val mGson by lazy(mode = LazyThreadSafetyMode.NONE) { Gson() }

    override fun convert(output: Any?, mediaType: MediaType?): ResponseBody {
        if (output is HttpResponseEntity<*>) {
            val json = mGson.toJson(output)
            return HttpResponseEntityBody(json)
        } else {
            throw NotImplementedError()
        }
    }

    override fun <T : Any?> convert(stream: InputStream, mediaType: MediaType?, type: Type): T? {
        val charset: Charset = mediaType?.charset
            ?: return JsonUtils.parseJson(IOUtils.toString(stream), type)
        return JsonUtils.parseJson(IOUtils.toString(stream, charset), type)
    }
}