package com.liskovsoft.smartyoutubetv2.mobile.update

import androidx.core.content.FileProvider

/** Own subclass so it never clashes with FileProviders declared by the libraries */
class UpdateFileProvider : FileProvider()
