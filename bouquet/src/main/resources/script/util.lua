return {
    assertType = function(i, input, class)
        assert(java.instanceOf(input, class), "Argument #"..i..": Expected type "..class:name()..", got ".. type(input))
    end,
    assertServer = function()
        assert(package.environment() == "server", "Operation cannot be performed on client")
    end,
    holders = {
        argumentTypes = {},
        commands = {}
    }
}
