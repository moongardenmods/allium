local Tag = require("net.minecraft.nbt.Tag")
local NumericTag = require("net.minecraft.nbt.NumericTag")
local DoubleTag = require("net.minecraft.nbt.DoubleTag")
local ByteTag = require("net.minecraft.nbt.ByteTag")
local IntTag = require("net.minecraft.nbt.IntTag")
local LongTag = require("net.minecraft.nbt.LongTag")
local ByteArrayTag = require("net.minecraft.nbt.ByteArrayTag")
local IntArrayTag = require("net.minecraft.nbt.IntArrayTag")
local LongArrayTag = require("net.minecraft.nbt.LongArrayTag")
local StringTag = require("net.minecraft.nbt.StringTag")
local ListTag = require("net.minecraft.nbt.ListTag")
local CompoundTag = require("net.minecraft.nbt.CompoundTag")

local util = require("bouquet.util")

local function fromTagInternal(element)
    local tag = element:getId()
    if tag == Tag.TAG_BYTE or tag == Tag.TAG_SHORT or tag == Tag.TAG_INT then
        return java.cast(element, NumericTag):intValue()
    elseif tag == Tag.TAG_LONG then
        return java.cast(element, LongTag):longValue()
    elseif tag == Tag.TAG_FLOAT or tag == Tag.TAG_DOUBLE then
        return java.cast(element, NumericTag):doubleValue()
    elseif tag == Tag.TAG_BYTE_ARRAY then
        return java.cast(element, ByteArrayTag):getAsByteArray()
    elseif tag == Tag.TAG_INT_ARRAY then
        return java.cast(element, IntArrayTag):getAsIntArray()
    elseif tag == Tag.TAG_LONG_ARRAY then
        return java.cast(element, LongArrayTag):getAsLongArray()
    elseif tag == Tag.TAG_STRING then
        return element:asString():orElseThrow()
    elseif tag == Tag.TAG_LIST then
        local list = java.cast(element, ListTag)
        local out = {}
        for i = 0, i < list:size() do
            out[i+1] = fromTagInternal(list:get(i))
        end
        return out
    elseif tag == Tag.TAG_COMPOUND then
        local list = java.cast(element, CompoundTag)
        local out = {}
        list:keySet():forEach(function(key)
            out[key] = fromTagInternal(list:get(key))
        end)
    else
        return nil
    end
end

local function toTagInternal(value, seenValues)
    if java.instanceof(value, Tag) then
        return value
    elseif seenValues[value] then
        return nil
    elseif type(value) == "number" and math.floor(value) == value then
        return IntTag.valueOf(value)
    elseif type(value) == "number" then
        return DoubleTag.valueOf(value)
    elseif type(value) == "boolean" then
        return ByteTag.valueOf(value)
    elseif type(value) == "string" then
        return StringTag.valueOf(value)
    elseif type(value) == "table" then
        local nbt = CompoundTag()
        seenValues[value] = true
        for k, v in pairs(value) do
            local nv = toTagInternal(v, seenValues)
            if nv then
                nbt:put(tostring(k), nv)
            end
        end
        seenValues[value] = nil
        return nbt
    else
        return nil
    end
end

return {
    fromTag = function(element)
        util.assertType(1, element, Tag)
        return fromTagInternal(element)
    end,
    toTagSafe = function(value)
        local ok, out = pcall(toTagInternal, value, {})
        if ok then return out end
    end,
    toTag = function(value)
        return toTagInternal(value, {})
    end
}