/**
 *  Porest
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
public static String version() { return "v0.0.1.20200622" }
/*
 *   2020/06/22 >>> v0.0.1.20200622 - Initialize
 */
import groovy.json.*
import groovy.json.JsonSlurper

metadata {
    definition(name: "Porest", namespace: "ktj1312", author: "ktj1312", vid: "SmartThings-Porest", ocfDeviceType: "x.com.st.d.airqualitysensor") {
        capability "Air Quality Sensor"
        capability "Carbon Monoxide Measurement"    // co
        capability "Carbon Dioxide Measurement"     // co2
        capability "Dust Sensor"                    // pm10
        capability "Fine Dust Sensor"               // pm2_5
        capability "Temperature Measurement"        // temperature
        capability "Relative Humidity Measurement"  // humidity
        capability "Tvoc Measurement"               // tvoc
        capability "Sensor"

        command "refresh"
    }

    preferences {
        input "devMuid", "text", type: "text", title: "디바이스 MUID", description: "enter device muid", required: true
        input "devSecret", "text", type: "text", title: "디바이스 Secret", description: "enter device secret", required: true
        input type: "paragraph", element: "paragraph", title: "Version", description: version(), displayDuringSetup: false
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles {
        multiAttributeTile(name: "Temperature", type: "generic", width: 6, height: 4) {
            tileAttribute("device.Temperature", key: "PRIMARY_CONTROL") {
                attributeState('default', label: '${currentValue}')
            }

            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
                attributeState("default", label: 'Update time : ${currentValue}')
            }
        }

        valueTile("temperature_value", "device.temperature") {
            state "default", label: '${currentValue}°'
        }

        valueTile("humidity_value", "device.humidity", decoration: "flat") {
            state "default", label: '${currentValue}%'
        }

        valueTile("co2_value", "device.carbonDioxide", decoration: "flat") {
            state "default", label: '${currentValue}'
        }

        valueTile("co_value", "device.carbonMonoxide", decoration: "flat") {
            state "default", label: '${currentValue}'
        }

        valueTile("voc_value", "device.tvocLevel", decoration: "flat") {
            state "default", label: '${currentValue}'
        }

        valueTile("pm10_value", "device.DustLevel", decoration: "flat") {
            state "default", label: '${currentValue}', unit: "㎍/㎥"
        }

        valueTile("pm25_value", "device.fineDustLevel", decoration: "flat") {
            state "default", label: '${currentValue}', unit: "㎍/㎥"
        }

        standardTile("refresh_air_value", "", width: 1, height: 1, decoration: "flat") {
            state "default", label: "", action: "refresh", icon: "st.secondary.refresh"
        }

        main(["Temperature"])
        details([
                "Temperature",
                "temperature_value",
                "humidity_value",
                "co2_value",
                "voc_value",
                "pm25_value",
                "refresh_air_value"
        ])
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

def installed() {
    log.debug "installed()"
    init()
}

def uninstalled() {
    log.debug "uninstalled()"
    unschedule()
}

def updated() {
    log.debug "updated()"
    unschedule()
    init()
}

def init(){
    refresh()
    //schedule("0 0/1 * * * ?", refresh)
    runEvery1Minute(refresh)
}

def refresh() {
    log.debug "refresh()"

    if(devMuid || devSecret){
        updateAirData()
        def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        sendEvent(name: "lastCheckin", value: now, displayed: false)
    }
    else log.error "Missing settings devMuid or devSecret"
}

def updateAirData(){
    def options = [
            "uri":"https://onem2m.medbiz.or.kr/Mobius/" + "${devMuid}" + "/fields/data/latest",
            "method": "GET",
            "headers": [
                    "X-M2M-Origin": "${devSecret}",
                    "X-M2M-RI":"SmartThings" + "${devMuid}"
            ]
    ]

    def respMap = getHttpGetJson(options)
    updateAirdataValues(respMap)

}

def updateAirdataValues(resp){

    def msg
    try {
        msg = parseLanMessage(hubResponse.description)

        def resp = new JsonSlurper().parseText(msg.body)

        sendEvent(name: "temperature", value: resp.temperature, displayed: true)
        sendEvent(name: "humidity", value: resp.humidity, displayed: true)
        sendEvent(name: "carbonDioxide", value: resp.co2, displayed: true)
        sendEvent(name: "tvocLevel", value: resp.tvoc, displayed: true)
        sendEvent(name: "fineDustLevel", value: resp.pm2_5, displayed: true)

    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

private getHttpGetJson(param) {
    log.debug "getHttpGetJson>> params : ${param}"
    def jsonMap = null
    try {
        httpGet(param) { resp ->
            log.debug "getHttpGetJson>> resp: ${resp.data}"
            jsonMap = resp.data
        }
    } catch(groovyx.net.http.HttpResponseException e) {
        log.error "getHttpGetJson>> HTTP Get Error : ${e}"
    }

    return jsonMap

}