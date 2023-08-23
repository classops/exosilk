package com.github.classops.silkdemo

import com.github.classops.exoplayer.ext.silk.SilkExtractor
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.extractor.ExtractorsFactory

class MyExtractorsFactory : ExtractorsFactory {

    private val extractorsFactory = DefaultExtractorsFactory()

    override fun createExtractors(): Array<Extractor> {
        val list = arrayListOf<Extractor>(
            SilkExtractor()
        )
        list.addAll(extractorsFactory.createExtractors())
        return list.toTypedArray()
    }

}