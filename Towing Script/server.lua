TMC.Functions.RegisterServerEvent('towing:server:getRandomNum', function(src, val1, val2)
    local rand = TMC.Common.TrueRandom(val1, val2)
end)

RegisterCommand('getbucket', function(source)
    local id = GetPlayerRoutingBucket(source)
end)

TMC.Functions.RegisterServerEvent('jobs:towing:towRepIncrease', function(src, partyId)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    for k,v in pairs(partyInfo.members) do
        local ply = TMC.Functions.GetPlayer(k)
        if ply then
            ply.Functions.AddReputation('towing', 1)
        end
    end

end)

TMC.Functions.RegisterServerEvent('jobs:towing:towPayout', function(src, lostTowVehicle, jobTier, partyId)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    if partyInfo and partyInfo.type == 'towing' then
        if not partyInfo.members[src] then
            print('[JOBS-ERROR] Player is not a member of the party for towing job')
            return
        end
        local totalAmount = 0
        local payoutPerPlayer = 0
        if lostTowVehicle then
            totalAmount = (Config.Towing.basepay * Config.Towing.StartLocations[jobTier].PayMultiplier * (partyInfo.data.carsTowed - partyInfo.data.superCount) * partyInfo.playerCount - Config.Towing.deposit)
            if partyInfo.data.superCount > 0 then
                for i = 0, spartyInfo.data.superCount, 1 do
                    totalAmount = totalAmount + (Config.Towing.basepay * (Config.Towing.StartLocations[jobTier].PayMultiplier + 0.5))
                end
            end
            payoutPerPlayer = totalAmount / partyInfo.playerCount
        else
            totalAmount = (Config.Towing.basepay * Config.Towing.StartLocations[jobTier].PayMultiplier * (partyInfo.data.carsTowed - partyInfo.data.superCount) * partyInfo.playerCount)
            if partyInfo.data.superCount > 0 then
                for i = 0, partyInfo.data.superCount, 1 do
                    totalAmount = totalAmount + (Config.Towing.basepay * (Config.Towing.StartLocations[jobTier].PayMultiplier + 0.5))
                end
            end
            payoutPerPlayer = totalAmount / partyInfo.playerCount
        end
        for k,v in pairs(partyInfo.members) do
            local player = TMC.Functions.GetPlayer(k)
            if player then
                if payoutPerPlayer > 0 then
                    player.Functions.AddMoney('bank', payoutPerPlayer, 'Towing Payout')
                    TriggerClientEvent('TMC:SimpleNotify', k, 'We\'ve deposited your pay of $'..payoutPerPlayer..' into your bank account', 'success', 7500)
                elseif payoutPerPlayer < 0 then
                    TriggerClientEvent('TMC:SimpleNotify', k, 'You still owed us for a truck you lost. Your paycheck has gone towards your debt', 'error', 7500)
                else
                    TriggerClientEvent('TMC:SimpleNotify', k, 'No pay to deposit', 'error', 7500)
                end
                TriggerClientEvent("towing:client:setEndVariables", k)
            end
        end
        TriggerEvent('parties:server:endActivityParty', partyId)
    end
end)

Citizen.CreateThread(function()
	local ZoneInUse = {}
	for zone,data in pairs(Config.Towing.DropLocations['salvage']) do
        for k, v in pairs(data) do
			ZoneInUse[v] = false
		end
	end
    for zone,data in pairs(Config.Towing.DropLocations['repair']) do
        for k, v in pairs(data) do
			ZoneInUse[v] = false
		end
	end
    for zone,data in pairs(Config.Towing.DropLocations['impound']) do
        for k, v in pairs(data) do
			ZoneInUse[v] = false
		end
	end
	GlobalState.VehicleTowingZonesInUse = ZoneInUse
end)

TMC.Functions.RegisterServerEvent("jobs:towing:manageZoneInUse", function(source, zone, partyId, b)
	local zoneData = TMC.Common.CopyTable(GlobalState.VehicleTowingZonesInUse)
    if type(zone) == 'table' then
        for k,v in pairs(zone) do
            zoneData = ZoneCleanUp(zoneData, v)
        end
    else
        zoneData = ZoneCleanUp(zoneData, zone, source, partyId, b)
    end
	GlobalState.VehicleTowingZonesInUse = zoneData
end)

function ZoneCleanUp(zoneData,zone,source,partyId,b)
	if zoneData[zone] == nil then return; end
	if b then
		zoneData[zone] = source
        local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
        for k,v in pairs(partyInfo.members) do
		    TriggerClientEvent("towing:client:createTowZone", k, zone)
        end
	else
		zoneData[zone] = false
	end
    return zoneData
end

TMC.Functions.RegisterServerEvent("jobs:towing:createPartyImpoundZones", function(source, partyId)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(500)
    for k,v in pairs(partyInfo.members) do
		TriggerClientEvent("towing:client:createImpoundZones", k)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:removePartyImpoundZones", function(source, partyId)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(500)
    for k,v in pairs(partyInfo.members) do
		TriggerClientEvent("towing:client:removeImpoundZones", k)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:setPartyVehicles", function(source, partyId, curTow, curStart, towV, veh)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    for k,v in pairs(partyInfo.members) do
		TriggerClientEvent("towing:client:setVehicles", k, curTow, curStart, towV, veh)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:setPartyVehicleStats", function(source, partyId, veh, vin, hasCar)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    for k,v in pairs(partyInfo.members) do
        TriggerClientEvent("towing:client:setVehicleStats", k, veh, hasCar)
        TriggerClientEvent('vehiclelock:client:setLockStatus', k, vin, false, 'outside', true)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:setPartyInZone", function(source, partyId, b)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    for k,v in pairs(partyInfo.members) do
		TriggerClientEvent("towing:client:setInZone", k, b)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:setPartyRestockVariables", function(source, partyId, veh)
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    for k,v in pairs(partyInfo.members) do
        TriggerClientEvent("towing:client:setEndVariables", k)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:showPartyPrompt", function(source, partyId, starter, towStep)
    local b = false
    local count = 0
    if not partyId then
        local partyId = Player(source).state.party
    end
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(300)

    for k,v in pairs(partyInfo.members) do
        count = count + 1
    end

    if count < 2 then
        b = true
        TriggerClientEvent("towing:client:showPrompt", source, b, towStep)
    else
        for k,v in pairs(partyInfo.members) do
            if k ~= starter then
                b = true
            else
                b = false
            end
            TriggerClientEvent("towing:client:showPrompt", k, b, towStep)
        end
    end

end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:showPartyNotify", function(source, partyId)
    local count = 0
    if not partyId then
        local partyId = Player(source).state.party
    end
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(300)

    for k,v in pairs(partyInfo.members) do
        count = count + 1
    end
    local b = true

    if count > 1 then
        for k,v in pairs(partyInfo.members) do
            TriggerClientEvent("towing:client:showNotify", k, b)
        end
    end

end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:showPartyBlip", function(source, partyId, starter)
    local b = false

    if not partyId then
        local partyId = Player(source).state.party
    end

    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(300)
    for k,v in pairs(partyInfo.members) do
        if k == starter then
            b = true
        else
            b = false
        end
        TriggerClientEvent("towing:client:showBlip", k, b, starter)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:setPartyStarter", function(source, partyId, curTow, starter)
    if not partyId then
        local partyId = Player(source).state.party
    end

    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(300)
    for k,v in pairs(partyInfo.members) do
        TriggerClientEvent("towing:client:setStarter", k, curTow, starter)
    end
end)


TMC.Functions.RegisterServerEvent("jobs:towing:server:attachPartyVehicle", function(source, partyId, curStart, starter)
    local b = false
    local count = 0
    if not partyId then
        local partyId = Player(source).state.party
    end

    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(300)


    for k,v in pairs(partyInfo.members) do
        count = count + 1
    end

    for k,v in pairs(partyInfo.members) do
        if count < 2 then
            b = false
        elseif k == starter then
            b = true
        else
            b = false
        end
		TriggerClientEvent("towing:client:attachVehicle", k, b, curStart, count)
    end
end)

TMC.Functions.RegisterServerEvent("jobs:towing:server:detachPartyVehicle", function(source, partyId, starter)
    local b = false
    local count = 0
    if not partyId then
        local partyId = Player(source).state.party
    end
    local partyInfo = TMC.Common.CopyTable(GlobalState['Party:'..tostring(partyId)])
    Wait(300)


    for k,v  in pairs(partyInfo.members) do
        count = count + 1
    end

    for k,v in pairs(partyInfo.members) do
        if count < 2 then
            b = false
        elseif k == starter then
            b = true
        else
            b = false
        end
		TriggerClientEvent("towing:client:detachVehicle", k, b, count)
    end
end)

TMC.Functions.RegisterServerCallback('jobs:towing:towingCheckVin', function(src, cb, vin)
    TMC.Functions.ExecuteSql("SELECT `vin` FROM `vehicles` WHERE `vin` = @vin", {
        ["@vin"] = vin
    }, function(result)
        if result[1] then
            cb(true)
        else
            cb(false)
        end
    end)
end)
