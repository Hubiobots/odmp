#  Copyright (c) 2020. James Adam and the Open Data Management Platform contributors.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# FieldDescription
# | type - One of STRING, NUMBER, BOOLEAN, ENUM, CODE
# | required - True or False
# | helperText - Optional helper text
# | options - List of available options (for an enum/select type)
# | properties - A Map of additional properties a plugin may need
codeField = {
    'type': 'CODE',
    'required': True,
    'helperText': 'The script to execute'
}

pluginFields = {
    'code': codeField
}

# PluginConfiguration
# | serviceName - The name of the service to be registered with consul
# | displayName - A pretty version of the service name to be displayed to users
# | type - A generic type for the processor describing its basic function
# | fields - A map of field descriptions for user configuration
config = {
    'serviceName': 'python-script-processor',
    'displayName': 'Python Script Processor',
    'type': 'SCRIPT',
    'fields': pluginFields
}