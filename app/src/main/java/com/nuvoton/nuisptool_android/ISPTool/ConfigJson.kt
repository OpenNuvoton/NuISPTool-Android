package com.nuvoton.nuisptool_android.ISPTool


import com.nuvoton.nuisptool_android.Util.Log
import kotlinx.serialization.*

//region: NuConfig, certain ispConfig depends on the chip
//use json template to build new empty configs




@Serializable
data class SubConfig(var configIndex: Int = 0, var index: Int = 0, val name: String, val description: String, val offset: Int, val length: Int, var values: String, var options: ArrayList<String>, var optionDescription: ArrayList<String>, var selectedOptionIndex: Int = 0) {
    override fun toString() : String {
        return  "configIndex: $configIndex, \n" +
                "name: $name, \n" +
                "description: $description, \n" +
                "offset: $offset, \n" +
                "length: $length, \n" +
                "values: $values, \n" +
                "=============== \n\n"
    }
}

@Serializable
data class SubConfigSet(val index: Int,val isEnable: Boolean, val subConfigs: ArrayList<SubConfig>) {
    override fun toString(): String {
        val subConfigString = subConfigs.map {
            "${it.name} to ${it.values}\n"
        }.joinToString()
        return "ispConfig$index: $subConfigString\n"
    }
}

@Serializable
data class IspConfig(val series:String, val subConfigSets: ArrayList<SubConfigSet>) {

}


