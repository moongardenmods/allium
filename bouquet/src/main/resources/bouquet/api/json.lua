local GsonBuilder = require("com.google.gson.GsonBuilder")
local JsonParser = require("com.google.gson.JsonParser")
local JsonElement = require("com.google.gson.JsonElement")
local JsonArray = require("com.google.gson.JsonArray")
local JsonObject = require("com.google.gson.JsonObject")
local JsonPrimitive = require("com.google.gson.JsonPrimitive")
local JsonNull = require("com.google.gson.JsonNull")

local util = require("bouquet.util")

local pretty = GsonBuilder():setPrettyPrinting():disableHtmlEscaping():create()
local compact = GsonBuilder():disableHtmlEscaping():create()

local function parseElement(element)
    if element == nil then return nil end
    local out = {}
    if element:isJsonObject() then
        local json = element.getAsJsonObject()
        json:entrySet():forEach(function(entry)
            out[entry:getKey()] = parseElement(entry:getValue())
        end)
    elseif element:isJsonArray() then
        local json = element:getAsJsonArray()
        json:forEach(function(value)
            out[#out+1] = value
        end)
    elseif element:isJsonPrimitive() then
        local primitive = element:getAsJsonPrimitive()
        if primitive:isBoolean() then
            return primitive:getAsBoolean()
        elseif primitive:isNumber() then
            return primitive:getAsDouble()
        elseif primitive:isString() then
            return primitive:getAsString()
        else
            return nil
        end
    else
        return nil
    end
    return out
end

local function parseLuaInternal(value, seenValues)
    if seenValues[value] then return JsonNull.INSTANCE end

    if type(value) == "userdata" and java.instanceOf(value, JsonElement) then
        return java.cast(value, JsonElement)
    elseif type(value) == "table" then
        local isArray = true
        seenValues[value] = true
        local tmp = {}
        for k, v in pairs(value) do
            if type(k) ~= "number" then isArray = false end
            tmp[k] = parseLuaInternal(v, seenValues)
        end
        local out
        if isArray then
            out = JsonArray()
            for i, v in ipairs(tmp) do
                out:add(v)
            end
        else
            out = JsonObject()
            for k, v in pairs(tmp) do
                out:add(tostring(k), v)
            end
        end
        seenValues[value] = nil
        return out
    elseif value == nil then
        return JsonNull.INSTANCE
    else
        return JsonPrimitive(value)
    end
end

local function parseLua(value)
    return parseLuaInternal(value, {})
end

return {
    pretty = pretty,
    compact = compact,
    fromJson = function(value)
        util.assertType(1, value, function()
            return type(value) == "string" or java.instanceOf(value, JsonElement)
        end)
        if type(value) == "string" then
            return parseElement(JsonParser.parseString(value))
        else
            return parseElement(value)
        end
    end,
    toJson = function(value, squish)
        return (squish and compact or pretty):toJson(parseLua(value))
    end,
    toJsonElement = parseLua
}