return {
    assertType = function(i, input, class)
        assert(class, "bouquet internal - cannot assert type - missing class argument")
        assert(input and java.instanceOf(input, class), "Argument #"..i..": Expected type "..class.class:getName()..", got ".. type(input))
    end,
    assertServer = function()
        assert(package.environment() == "server", "Operation cannot be performed on client")
    end,
    holders = {
        argumentTypes = {},
        commands = {}
    }
}
