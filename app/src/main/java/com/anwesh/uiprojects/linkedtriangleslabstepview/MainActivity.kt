package com.anwesh.uiprojects.linkedtriangleslabstepview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.triangleslabstepview.TriangleSlabStepView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TriangleSlabStepView.create(this)
    }
}
