/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

var resources = function (page, meta) {
    return {
        //js: ['alldevices_util.js']
        css:['select2.min.css', 'custom-item.css'],
        js: ['device-listing.js','libs/handlebars-v2.0.0.js','libs/utils.js',
            'libs/select2.full.min.js','libs/jquery-ui.js',
            'libs/invoker-lib.js','libs/js.cookie.js']
    };
};