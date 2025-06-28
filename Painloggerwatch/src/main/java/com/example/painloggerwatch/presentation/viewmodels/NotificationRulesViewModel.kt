package com.example.painloggerwatch.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.painlogger.data.NotificationRule

class NotificationRulesViewModel : ViewModel() {
    private val _rules = MutableLiveData<List<NotificationRule>>(emptyList())
    val rules: LiveData<List<NotificationRule>> = _rules

    fun addRule(rule: NotificationRule) {
        _rules.value = _rules.value.orEmpty() + rule
    }

    fun getRulesByType(type: String): List<NotificationRule> {
        return _rules.value.orEmpty().filter { it.type == type }
    }

    fun removeRule(id: Long) {
        _rules.value = _rules.value.orEmpty().filterNot { it.id == id }
    }

}