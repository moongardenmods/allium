return {
    assertType = function(i, input, class)
        assert(class, "bouquet internal - cannot assert type - missing class argument")
        local errStr = "Argument #"..i..": Expected type "..class.class:getName()..", got ".. type(input)
        if type(class) == "function" then
            assert(input and class(), errStr)
        else
            assert(input and java.instanceOf(input, class), errStr)
        end
    end,
    assertServer = function()
        assert(package.environment() == "server", "Operation cannot be performed on client")
    end,
    holders = {
        argumentTypes = {},
        commands = {}
    }
}
