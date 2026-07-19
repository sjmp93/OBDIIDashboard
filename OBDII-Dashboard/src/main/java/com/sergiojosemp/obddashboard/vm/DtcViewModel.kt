package com.sergiojosemp.obddashboard.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DtcViewModel : ViewModel() {

    private val _dtcCodes = MutableLiveData<List<String>>()
    val dtcCodes: LiveData<List<String>> = _dtcCodes

    private val _outputText = MutableLiveData<String>()
    val outputText: LiveData<String> = _outputText

    private val _isFetchingDtc = MutableLiveData<Boolean>()
    val isFetchingDtc: LiveData<Boolean> = _isFetchingDtc

    private val _showResults = MutableLiveData<Boolean>()
    val showResults: LiveData<Boolean> = _showResults

    fun setDtcCodes(codes: List<String>) {
        _dtcCodes.value = codes
        _isFetchingDtc.value = false
        _showResults.value = true
    }

    fun clearDtcCodes() {
        _dtcCodes.value = emptyList()
        _showResults.value = false
    }

    fun setOutputText(text: String) {
        _outputText.value = text
    }

    fun startFetchingDtc() {
        _isFetchingDtc.value = true
    }

    fun showDtcResults() {
        _showResults.value = true
    }

    fun setShowResults(show: Boolean) {
        _showResults.value = show
    }
}
