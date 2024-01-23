local createdTowBlips, towLocationsUsed, towZonesUsed, impoundSpawns, impoundSpawnZones, impoundSpawnCoords, impoundPZs, blipTable = {}, {}, {}, {}, {}, {}, {}, {}
local towingRep, totalTowSteps, totalTowZones, stepsPerTowZone, remainTowStepsPerTowZone, curTowStep, prevTowStep, curTowMission, curTowZone, curTowBlip, onTow, towRepCounter, lostTowVehicle, returnTowBlip = nil, nil, nil, nil, nil, 0, 0, nil, nil, nil, false, 0, false, nil
local towVeh, towVin, vehToTow, vehToTowVin, oldVehToTow, vehToSpawn, hasCar, towHook, rope, rope2, isSuper  = nil, nil, nil, nil, nil, false, nil, nil, nil, nil
local towingStartPrompt, towingEndPrompt, towingDropPrompt, towLoadingZonePrompt = nil, nil, nil, nil
local curTowStart, towAssigned, towPZ, towPZ2, truckPZ, firstTow = nil, nil, nil, nil, nil, nil
local towingMarkerThread, returnMarkerThread, truckTrackerThread, loadTowVehicleTickThread, inTowZone = false, false, false, false, false
local playerId, playerPedId, position, towCoords, doTowCount, impoundCount, hookTowCount, closestVehicle = nil, nil, nil, nil, 0, 0, 0, nil

local lastTowParty = nil
local towStarter = nil
local showPrompt, showBlip = nil, nil
local playerTowPartyChangeHandler = nil
local carsTowed = 0
local superCount = 0
local impoundZones, impoundBlip, plantCarSpawned, plantCar, justAttached = nil, nil, false, false, false

function HandleTowingParty(partyId)
    if lastTowParty ~= nil and partyId == nil then
        TowPartyCleanup(true, lastTowParty)
    elseif partyId ~= nil then
        if lastTowParty ~= partyId then
            TowPartyCleanup(true, partyId)
        end
        local partyKey = 'Party:'..tostring(partyId)
        if GlobalState[partyKey] ~= nil and GlobalState[partyKey].type == 'towing' then
            -- join new party and start thread
            StartTow()
        end
    end
    lastTowParty = partyId
end

Citizen.CreateThread(function()
    while TMC == nil do
        Citizen.Wait(1000)
    end

    playerId, playerPedId, position = TMC.Functions.GetLocalData()

    while not TMC.Functions.IsPlayerLoaded() do
        Citizen.Wait(50)
    end
    Citizen.Wait(200)
    towingRep = LocalPlayer.state.rep['towing']
    HandleTowingBlips()
    playerTowPartyChangeHandler = AddStateBagChangeHandler('party', string.format('player:%s', GetPlayerServerId(PlayerId())), function(bag, key, value)
        HandleTowingParty(value)
    end)

    for k, v in pairs(Config.Towing.StartLocations) do
        SetRoadsInArea(v.StartZone[1] - vector3(0, 0, 20), v.StartZone[2] + vector3(0, 0, 20), false)
        SetPedPathsInArea(v.StartZone[1] - vector3(0, 0, 20), v.StartZone[2] + vector3(0, 0, 20), false)
        local startCoords = v.Location.xyz
        if v.Ped ~= nil then
            startCoords = TMC.Natives.GetOffsetFromCoordsInDirection(v.Location.xyz, v.Location.w, vector3(0.0, 1.0, 0.0))
            TMC.Functions.CreateInteractionPed('towingstart_' .. k, {
                Hash = v.Ped,
                Location = v.Location - vector4(0.0, 0.0, 1.0, 0.0),
                Scenario = 'WORLD_HUMAN_CLIPBOARD'
            })
        end
        TMC.Functions.AddCircleZone('towstart', startCoords, 1.0, {
            useZ = true,
            data = {
                ['tow'] = k,
            }
        })
        TMC.Functions.AddPolyZone('towingunload', v.LoadingZone, {
            useZ = false,
            data = {
                ['tow'] = k
            }
        })
        TMC.Functions.AddPolyZone('towingreturn', v.ReturnZone, {
            useZ = false,
            data = {
                ['tow'] = k
            }
        })
    end

    towingStartPrompt = TMC.Functions.CreatePromptGroup({{
        Id = 'salvage_route',
        Complete = function()
            if TMC.Functions.IsOnDuty() then
                TMC.Functions.SimpleNotify('You cannot do towing if you are clocked-in on another job', 'error')
                return
            end
            if not onTow then
                local foundSpot = false
                for k, v in ipairs(Config.Towing.StartLocations[curTowStart].VehSpawn) do
                    if TMC.Functions.IsSpawnPointClear(v.xyz, 3) then
                        onTow = true
                        SetupTow(curTowStart, 'salvage', 'initial', v)
                        DrawTowVehMarker(v)
                        truckPZ = TMC.Functions.AddCircleZone('towtruck', v, 4.0, {
                            useZ = false,
                            data = {
                                ['tow'] = k,
                                id = 'towtruck'
                            }
                        })
                        TMC.Functions.HidePromptGroup(towingStartPrompt)
                        foundSpot = true
                        towStarter = GetPlayerServerId(playerId)
                        TMC.Functions.Notify({
                            message = 'If you are towing with a friend make sure you invite them to the party before approaching your tow truck!',
                            id = 'partynotify',
                            persist = true,
                            notifType = 'info'
                        })
                        break
                    end
                end
                if not foundSpot then
                    TMC.Functions.SimpleNotify('All loading bays are currently occupied', 'error')
                end
            end
        end,
        Title = 'Salvage Route',
        AutoComplete = false,
        Description = 'Approximately 5-10 cars need towing',
        Icon = 'fa-duotone fa-truck-tow'
    },{
        Id = 'repair_route',
        Complete = function()
            if TMC.Functions.IsOnDuty() then
                TMC.Functions.SimpleNotify('You cannot do towing if you are clocked-in on another job', 'error')
                return
            end
            if not onTow then
                local foundSpot = false
                for k, v in ipairs(Config.Towing.StartLocations[curTowStart].VehSpawn) do
                    if TMC.Functions.IsSpawnPointClear(v.xyz, 3) then
                        onTow = true
                        SetupTow(curTowStart, 'repair', 'initial', v)
                        DrawTowVehMarker(v)
                        truckPZ = TMC.Functions.AddCircleZone('towtruck', v, 4.0, {
                            useZ = false,
                            data = {
                                ['tow'] = k,
                                id = 'towtruck'
                            }
                        })
                        TMC.Functions.HidePromptGroup(towingStartPrompt)
                        foundSpot = true
                        towStarter = GetPlayerServerId(playerId)
                        TMC.Functions.Notify({
                            message = 'If you are towing with a friend make sure you invite them to the party before approaching your tow truck!',
                            id = 'partynotify',
                            persist = true,
                            notifType = 'info'
                        })
                        break
                    else
                    end
                end
                if not foundSpot then
                    TMC.Functions.SimpleNotify('All loading bays are currently occupied', 'error')
                end
            end
        end,
        Title = 'Repair Route',
        AutoComplete = false,
        Description = 'Approximately 5-10 cars need towing',
        Icon = 'fa-duotone fa-truck-tow'
    },{
        Id = 'impound_route',
        Complete = function()
            if TMC.Functions.IsOnDuty() then
                TMC.Functions.SimpleNotify('You cannot do towing if you are clocked-in on another job', 'error')
                return
            end
            if not onTow then
                local foundSpot = false
                for k, v in ipairs(Config.Towing.StartLocations[curTowStart].VehSpawn) do
                    if TMC.Functions.IsSpawnPointClear(v.xyz, 3) then
                        onTow = true
                        SetupTow(curTowStart, 'impound', 'initial', v)
                        DrawTowVehMarker(v)
                        truckPZ = TMC.Functions.AddCircleZone('towtruck', v, 4.0, {
                            useZ = false,
                            data = {
                                ['tow'] = k,
                                id = 'towtruck'
                            }
                        })
                        TMC.Functions.HidePromptGroup(towingStartPrompt)
                        foundSpot = true
                        towStarter = GetPlayerServerId(playerId)
                        TMC.Functions.Notify({
                            message = 'If you are towing with a friend make sure you invite them to the party before approaching your tow truck!',
                            id = 'partynotify',
                            persist = true,
                            notifType = 'info'
                        })
                        break
                    else
                    end
                end
                if not foundSpot then
                    TMC.Functions.SimpleNotify('All loading bays are currently occupied', 'error')
                end
            end
        end,
        Title = 'Impound Route',
        AutoComplete = false,
        Description = 'There are some cars that need towing',
        Icon = 'fa-duotone fa-truck-container-empty'
    },{
        Id = 'towing_clothing',
        Complete = function()
            TriggerEvent("clothing:client:openJobOutfitsOnly", Config.Towing.StartLocations[curTowStart].OutfitType)
        end,
        Title = 'Job Outfit',
        Icon = 'fad fa-tshirt',
        AutoComplete = true
    },{
        Id = 'end_tow_no_veh',
        Complete = function()
            lostTowVehicle = true
            onTow = false
            DoTowCleanup('end')
            TMC.Functions.RemoveBlip(curTowBlip)
            TMC.Functions.HidePromptGroup(towingStartPrompt)
            TMC.Functions.TriggerServerEvent('parties:server:leaveParty')
        end,
        Title = 'End Towing (Lost Vehicle)',
        Description = 'Are you sure you\'ve lost your vehicle? We take a $'..Config.Towing.deposit..' deposit off of your pay for that',
        Icon = 'fa-duotone fa-circle-xmark'
    }})

    towingLoadCarPrompt = TMC.Functions.CreatePromptGroup({{
        Id = 'load_car',
        Complete = function()
            TMC.Functions.TriggerServerEvent("jobs:towing:attachPartyVehicle", lastTowParty, curTowStart, towStarter)
            if curTowStart == 'impound' then
                TMC.Functions.StopNotify('findcar')
            end
        end,
        Title = 'Tow Vehicle',
        AutoComplete = false,
        Description = 'Load Car onto Tow Truck',
        Icon = 'fa-duotone fa-truck-tow'
    }})

    towingDropPrompt = TMC.Functions.CreatePromptGroup({{
        Id = 'drop_car',
        Complete = function()
            oldVehToTow = vehToTow
            local lowerArm = 0.0
            local raiseArm = 1.0

            if #(GetEntityCoords(vehToTow) - position) < 20.0 and #(GetEntityCoords(towVeh) - position) < 20.0 and curTowStep == prevTowStep and not IsPedInVehicle(playerPedId, towVeh, false) then
                if doTowCount == 0 then
                    doTowCount = 1
                    TMC.Functions.HidePromptGroup(towingDropPrompt)
                    TMC.Functions.TriggerServerEvent("jobs:towing:detachPartyVehicle", lastTowParty, towStarter)
                end
            elseif IsPedInVehicle(playerPedId, towVeh, false) then
                TMC.Functions.SimpleNotify('Get out of the tow truck to unhook the car.', 'error')
            else
                TMC.Functions.SimpleNotify('You do not have the vehicle I sent you for.', 'error')
            end

        end,
        Title = 'Drop Off Vehicle',
        AutoComplete = false,
        Description = 'Unload Car From Tow Truck',
        Icon = 'fa-duotone fa-truck-tow'
    }})

    towingEndPrompt = TMC.Functions.CreatePromptGroup({--[[{
        Id = 'new_tow_list',
        Complete = function()
            onTow = false
            SetBlipRoute(createdTowBlips[curTowMission], false)
            TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(oldVehToTow))
            SetupTow(curTowStart, 'salvage', 'restock', 1)
            TMC.Functions.HidePromptGroup(towingEndPrompt)
        end,
        Title = 'Get New Tow-List',
        AutoComplete = false,
        Description = 'Approximately 5 to 10 cars to tow before returning',
        Icon = 'fa-duotone fa-truck-arrow-right'
    }, ]]{
        Id = 'end_towing',
        Complete = function()
            onTow = false
            TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(oldVehToTow))
            DoTowCleanup('end')
            TMC.Functions.HidePromptGroup(towingEndPrompt)
            TMC.Functions.RemoveBlip(curTowBlip)
            TMC.Functions.TriggerServerEvent('parties:server:leaveParty')
        end,
        Title = 'Return Vehicle',
        AutoComplete = false,
        Description = 'Return the tow vehicle and get paid',
        Icon = 'fa-duotone fa-circle-xmark'
    }})

    TMC.Functions.AddPolyZoneEnterHandler('towstart', function(data)
        curTowStart = data.tow
        if curTowMission ~= nil or lastTowParty then
            TMC.Functions.ShowPromptGroup(towingStartPrompt, {'end_tow_no_veh'})
        else
            if Config.Towing.StartLocations[curTowStart].RepReq <= towingRep then
                if curTowStart == 'salvage' then
                    TMC.Functions.ShowPromptGroup(towingStartPrompt, {'salvage_route', 'towing_clothing'})
                elseif curTowStart == 'repair' then
                    TMC.Functions.ShowPromptGroup(towingStartPrompt, {'repair_route', 'towing_clothing'})
                elseif curTowStart == 'impound' then
                    TMC.Functions.ShowPromptGroup(towingStartPrompt, {'impound_route' , 'towing_clothing'})
                end
            else
                TMC.Functions.SimpleNotify('I haven\'t heard of you before. Try starting out with some simpler towing jobs first', 'speech', 10000)
            end
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('towstart', function(data)
        TMC.Functions.HidePromptGroup(towingStartPrompt)
    end)

    TMC.Functions.AddPolyZoneEnterHandler('towtruck', function(data)
        local config = Config.Towing
        TMC.Functions.RemoveBlip(curTowBlip)
        LoadTowVehicleTick()
        TMC.Functions.StopNotify('partynotify')
        if curTowStart == 'impound' then
            SpawnImpoundCars()
            TMC.Functions.TriggerServerEvent('jobs:towing:server:setPartyStarter', lastTowParty, curTowStart, towStarter)
            TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyVehicles", lastTowParty, curTowMission, curTowStart, VehToNet(towVeh), 'na') -- change vehtotow for impound
            TMC.Functions.TriggerServerEvent("jobs:towing:createPartyImpoundZones", lastTowParty)
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('towtruck', function(data)
        TMC.Functions.RemoveZoneById(truckPZ.id)
    end)

    TMC.Functions.AddPolyZoneEnterHandler('towingunload', function(data)
        if curTowMission ~= nil and removeTowedVehicleThread ~= true and lastTowParty then

            showPrompt = false
            TMC.Functions.TriggerServerEvent('jobs:towing:server:showPartyPrompt', lastTowParty, towStarter, prevTowStep)
            Wait(1000)

            if showPrompt == true then
                TMC.Functions.ShowPromptGroup(towingDropPrompt)
            end

            TMC.Functions.Notify({
                message = 'Alright, unhook the car and leave it here. We\'ll take care of it.',
                id = 'dropVehicle',
                persist = false,
                notifType = 'info'
            })
        end
            TMC.Functions.StopNotify('towzone')
    end)

    TMC.Functions.AddPolyZoneExitHandler('towingunload', function(data)
        doTowCount = 0
        if curTowMission == nil then
            TMC.Functions.HidePromptGroup(towingStartPrompt)
        elseif curTowMission == data.tow and totalTowSteps == nil then
            TMC.Functions.StopNotify('towdrop')
            TMC.Functions.HidePromptGroup(towingDropPrompt)
        end
        TMC.Functions.HidePromptGroup(towingDropPrompt)
    end)

    TMC.Functions.AddPolyZoneEnterHandler('towingreturn', function(data)
        if curTowMission == data.tow and totalTowSteps == nil and lastTowParty then
            if IsPedInVehicle(playerPedId, towVeh, false) then
                curTowStart = data.tow
                TMC.Functions.ShowPromptGroup(towingEndPrompt)
            else
                TMC.Functions.SimpleNotify('Please return here in your tow truck', 'error')
            end
        elseif curTowMission == data.tow and totalTowSteps ~= nil and IsPedInVehicle(playerPedId, towVeh, false) then
            TMC.Functions.ShowPromptGroup(towingEndPrompt, {'end_towing'})
        elseif curTowMission == data.tow and not IsPedInVehicle(playerPedId, towVeh, false) then
            TMC.Functions.SimpleNotify('Please return here in your tow truck', 'error')
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('towingreturn', function(data)
        TMC.Functions.HidePromptGroup(towingEndPrompt)
    end)

    TMC.Functions.AddPolyZoneEnterHandler('impoundzone', function(data)

        TMC.Functions.TriggerServerEvent("jobs:towing:setPartyInZone", lastTowParty, true)
        Wait(1000)
        local towRate = TMC.Common.TrueRandom(1, 100)
        local k = data.impoundzone
        local center = data.impoundzonecenter
        local inVeh = nil
        while inTowZone do
            inVeh = IsPedInVehicle(playerPedId, towVeh, false)
            if not inVeh then
                local vehiclesInArea = {}
                vehiclesInArea = TMC.Functions.GetVehiclesInArea(center, 5)
                if vehiclesInArea and lastTowParty then
                    TMC.Functions.SimpleNotify("Writing down plate for impound")
                    --[[
                        Check all vehicles closeby (inside the polyzone) to make sure that they aren't one of the following:
                        - Player Owned Vehicles
                        - Running/moving (being driven by locals)
                        - the tow truck itsself
                        If the nearest car is not one of these then it becomes the target vehicle and will be towable.
                    ]]
                    for k,v in pairs(vehiclesInArea) do
                        local vin = Entity(v).state.vin
                        TMC.Functions.TriggerServerCallback('jobs:towing:towingCheckVin', function(result)
                            if result ~= true and v ~= towVeh and GetIsVehicleEngineRunning(v) ~= true then
                                closestVehicle = v
                            elseif result then
                                TMC.Functions.SimpleNotify('This is a players car.', 'error')
                            end
                        end, vin)
                        if not vin then
                            if v ~= towVeh and GetIsVehicleEngineRunning(v) ~= true then
                                closestVehicle = v
                            end
                    end
                   end
                end
                break
            end
            Citizen.Wait(5)
        end
        Citizen.Wait(400)

        if closestVehicle then
            prevTowStep = curTowStep
            towPrompt()
        end

        for k,v in pairs(impoundSpawnZones) do
            if center == v.data.towzone then
                plantCar = true
                break
            end
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('impoundzone', function(data)
        plantCar = false

        TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyInZone", lastTowParty, false)
        Wait(1000)

        if justAttached == true then
            if IsEntityAttachedToEntity(vehToTow, towVeh) then
                hasCar = true
            else
                if GetEntityAttachedTo(towVeh) ~= 0 then
                    hasCar = true
                    vehToTow = GetEntityAttachedTo(towVeh)
                end
            end

            if hasCar == false and curTowStart == 'impound' then
                closestVehicle = nil
            end

            if hasCar == true then
                TMC.Functions.StopNotify('impoundzone')
                if towingRep < 275 then
                    TMC.Functions.Notify({
                        message = 'You found one, now carefully bring the car back to the lot.',
                        id = 'impoundzone',
                        persist = true,
                        notifType = 'info'
                    })
                end
                showBlip = false
                TMC.Functions.TriggerServerEvent('jobs:towing:server:showPartyBlip', lastTowParty, towStarter)
                Wait(1000)

                if showBlip == true then
                    --SetBlipRoute(curTowBlip, false)
                    --TMC.Functions.RemoveBlip(curTowBlip)
                    DrawReturnMarker()
                end
            end
            justAttached = false
        end
    end)

    TMC.Functions.AddPolyZoneEnterHandler('towspawnzone', function(data)
        if lastTowParty then
            if curTowStart ~= 'impound' then
                spawnTow()
            else
                if (plantCarSpawned == false) then
                    plantCarSpawned = true
                    TMC.Functions.SimpleNotify('I just got got a call that someone parked like an asshole, the location they gave is near you. Take a break from looking and find that car and bring it back to the lot', 'speech', 20000)
                    local vehTypeNum = TMC.Common.TrueRandom(1, #Config.Towing.vehToTowTypes[curTowStart])
                    vehToSpawn = Config.Towing.vehToTowTypes[curTowStart][vehTypeNum]
                    impoundSpawns[data.count] = TMC.Functions.SpawnVehicle(vehToSpawn, data.towzone, true)
                    impoundVin = TMC.Common.GenerateVIN(true)
                    while not impoundSpawns[data.count] do
                        Citizen.Wait(5)
                    end
                    local tempRand = TMC.Common.TrueRandom(30, 100)
                    impoundBlip = AddBlipForRadius(data.towzone.x + tempRand, data.towzone.y + tempRand, data.towzone.z + tempRand, 200.0)
                    SetBlipHighDetail(impoundBlip, true)
                    SetBlipColour(impoundBlip, 1)
                    SetBlipAlpha (impoundBlip, 128)
                    local blipData = {
                        blip = impoundBlip,
                        vehicle = impoundSpawns[data.count]
                    }
                    table.insert(blipTable, blipData)
                    if (hasCar == false) then
                        TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyVehicleStats", lastTowParty, VehToNet(impoundSpawns[data.count]), impoundVin, hasCar)
                    end
                    Wait(1000)
                    TMC.Functions.RemoveZoneById(impoundSpawnZones[data.count].id)
                end
            end
        end
    end)

    TMC.Functions.AddPolyZoneEnterHandler('towzone', function(data)

        if TMC.Functions.IsOnDuty() then
            TMC.Functions.SimpleNotify('this car is for towing! STAY AWAY', 'error')
        end
        prevTowStep = curTowStep

        if lastTowParty then
            SetVehicleDoorsLocked(vehToTow, 1)
            SetVehicleDoorsLockedForAllPlayers(vehToTow, false)
            TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyInZone", lastTowParty, true)
            TMC.Functions.TriggerServerEvent('jobs:towing:server:showPartyNotify', lastTowParty)
            Wait(1000)
            if curTowMission ~= nil then
                if towingRep < 15 then
                    TMC.Functions.Notify({
                        message = 'Find the car to be towed and hitch it up on your truck',
                        id = 'towzone',
                        persist = true,
                        notifType = 'info'
                    })
                end
                towPrompt()
            end
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('towzone', function(data)
        hookTowCount = 0
        TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyInZone", lastTowParty, false)
        Wait(1000)
        TMC.Functions.StopNotify('towzone')
        TMC.Functions.StopNotify('partydrivernotify')
        if towingRep < 15 then
            TMC.Functions.Notify({
                message = 'You got it, now bring the piece of junk back to the shop.',
                id = 'towzone',
                persist = true,
                notifType = 'info'
            })
        end
        if hasCar == true then
            showBlip = false
            TMC.Functions.TriggerServerEvent('jobs:towing:server:showPartyBlip', lastTowParty, towStarter)
            Wait(1000)

            if showBlip == true then
                SetBlipRoute(curTowBlip, false)
                TMC.Functions.RemoveBlip(curTowBlip)
                DrawReturnMarker()
            end
            SetVehicleBrake(vehToTow, false)
            SetVehicleHandbrake(vehToTow, false)

            TMC.Functions.RemoveZoneById(towPZ.id)
        end

        if hasCar == false and curTowStart == 'impound' then
            if towingRep < 300 then
                TMC.Functions.Notify({
                    message = 'Drive around the city and look for cars that are illegally parked.  If you find one get out of your car to check if we can tow it.  If we can load it up and bring it back here.',
                    id = 'impoundzone',
                    persist = true,
                    notifType = 'info'
                })
            end
        end
    end)

    TMC.Functions.AddPolyZoneEnterHandler('towdrop', function(data)
        TMC.Functions.StopNotify('impoundzone')
        if curTowMission ~= nil and data.sequence ~= curTowStep then
            TMC.Functions.SimpleNotify('This is not the right vehicle.', 'error')
        elseif curTowMission ~= nil then
            TMC.Functions.ShowPromptGroup(towingDropPrompt)
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('towdrop', function(data)
        TMC.Functions.StopNotify('towdrop')
        TMC.Functions.HidePromptGroup(towingDropPrompt)
    end)
end)

function SetupTow(id, towLength, towStartType, vehSpawn)
    local type = Config.Towing.StartLocations[id].Type
    curTowMission, onTow = id, true
    lostTowVehicle = false
    local config = Config.Towing
    totalTowSteps = TMC.Common.TrueRandom(config[towLength].MinSteps, config[towLength].MaxSteps)
    totalTowZones = TMC.Common.TrueRandom(config[towLength].MinZones, config[towLength].MaxZones)
    stepsPerTowZone = math.floor((totalTowSteps / totalTowZones) * 1 / 1)
    carsTowed = 0
    superCount = 0
    hasCar = false
    --if statement to set values for the first tow that won't get set if a player starts a new route without returning the truck
    if towStartType == 'initial' then
        if curTowStart ~= "impound" then
            TMC.Functions.SimpleNotify('These cars have been there a while, if the car won\'t move be sure you get in the drivers seat and check all the brakes.', 'speech', 20000)
        else
            TMC.Functions.SimpleNotify('People just can\'t stop parking like assholes in this city. I\'ve had enough. Take this truck and bring me back any car you see parked in a red zone', 'speech', 20000)
            Wait(2000)
            TMC.Functions.SimpleNotify('If someone calls in to report a car I\'ll shoot you an approximate location, but pick up any car you find. ', 'speech', 20000)
        end
        firstTow = true
        towVeh = TMC.Functions.SpawnVehicle(config.StartLocations[id].VehType, vehSpawn, true)
        SetVehicleOnGroundProperly(towVeh)
        while not DoesEntityExist(towVeh) do
            Citizen.Wait(0)
        end

        local netId = VehToNet(towVeh)
        SetVehicleLivery(towVeh, 0)
        SetVehicleEngineOn(towVeh, true, true)
        SetEntityVelocity(towVeh, 0.1, 0.1, 0.1)
        Citizen.Wait(300)
        if config.StartLocations[id].VehHint ~= nil then
            TMC.Functions.SimpleNotify(config.StartLocations[id].VehHint, 'speech')
        end
        towVin = TMC.Common.GenerateVIN(true)
        TriggerEvent('vehiclelock:client:addKeys', towVeh, towVin)
        TMC.Functions.TriggerServerEvent('TMC:SetEntityStateBag', netId, 'vin', towVin)
        TMC.Functions.TriggerServerEvent("TMC:SetEntityStateBag", netId, 'fuel', 100)
        TMC.Functions.TriggerServerEvent('parties:server:createActivityParty', 'Towing Runs', 'towing', 2, {
            vehicle = towVeh,
            state = 'starting',
            hasCar = hasCar,
            carsTowed = carsTowed,
            superCount = superCount
        })
        curTowZone = TMC.Common.TrueRandom(1, 5)
        towZonesUsed[curTowZone] = true
        curTowStep = 1
        prevTowStep = 1
        remainTowStepsPerTowZone = stepsPerTowZone
        returnTowBlip = TMC.Functions.CreateBlip('Return Towtruck', config.StartLocations[curTowStart].returnLoc, 524, 4, 'Return')
    elseif towStartType == 'restock' then
        TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyRestockVariables", lastTowParty)
        hasCar = false
        foundSpot = false
        towStarter = nil
        curTowZone = TMC.Common.TrueRandom(1, 5)
        towZonesUsed[curTowZone] = true
        curTowStep = 1
        prevTowStep = 1
        remainTowStepsPerTowZone = stepsPerTowZone
        LoadTowVehicleTick()
    end
end

RegisterNetEvent('towing:client:createImpoundZones', function()
    local count = 1
    impoundZones = Config.Towing.DropLocations['impound']
    impoundCount = 1
    for k,v in pairs(impoundZones) do
        for i,j in pairs (v) do
            impoundPZs[count] = TMC.Functions.AddCircleZone('impoundzone', j, 5, {
                useZ = false,
                data = {
                    ['tow'] = curTowMission,
                    ['sequence'] = curTowStep,
                    ['impoundzonecenter'] = impoundZones[k][i],
                    ['impoundzone'] = i
                }
            })
            count = count + 1
        end
    end
end)

RegisterNetEvent('towing:client:removeImpoundZones', function()
    for k,v in pairs(impoundPZs) do
        TMC.Functions.RemoveZoneById(impoundPZs[k].id)
    end
end)

function RequestEntityControl(entity)
    local control = NetworkHasControlOfEntity(entity)
    if not control then
        NetworkRequestControlOfEntity(entity)
        SetTimeout(1000, function()
            control = true
        end)
        while not NetworkHasControlOfEntity(entity) and not control do
            NetworkRequestControlOfEntity(entity)
            Wait(50)
        end
    end
    return NetworkHasControlOfEntity(entity)
end

--Function to set up a set amount of polyzones that will spawn cars for the tier 3 towing so some pickups will be guaranteed in the case of local spawns being slow
function SpawnImpoundCars()
    local config = Config.Towing
    local maxCars = TMC.Common.TrueRandom(config['impound'].MinSpawns, config['impound'].MaxSpawns)
    while impoundCount < maxCars do
        local foundSpot = false
        while foundSpot == false do
            local impoundSpawnZone = TMC.Common.TrueRandom(1, #Config.Towing.DropLocations[curTowStart])
            local impoundSpawnLoc = TMC.Common.TrueRandom(1, #Config.Towing.DropLocations[curTowStart][impoundSpawnZone])
            impoundCoords = Config.Towing.DropLocations[curTowStart][impoundSpawnZone][impoundSpawnLoc]
            local vehTypeNum = TMC.Common.TrueRandom(1, #Config.Towing.vehToTowTypes[curTowStart])
            vehToSpawn = Config.Towing.vehToTowTypes[curTowStart][vehTypeNum]
            if TMC.Functions.IsSpawnPointClear(impoundCoords.xyz, 3) and GlobalState.VehicleTowingZonesInUse[impoundCoords] ~= true then
                impoundSpawnCoords[impoundCount] = impoundCoords
                impoundSpawnZones[impoundCount] = TMC.Functions.AddCircleZone('towspawnzone', impoundCoords, 200.0, {
                    useZ = false,
                    data = {
                        ['tow'] = curTowMission,
                        ['sequence'] = curTowStep,
                        ['towzone'] = impoundCoords,
                        ['count'] = impoundCount
                    }
                })
                foundSpot = true
                impoundCount = impoundCount + 1
            end
            Wait(5)
        end
        Wait(5)
    end

end

function SpawnAttackPed(coords)
    local pedType = 'a_m_y_business_03'
    local foundPedSpot = false
    local pedCoords = nil
    while foundPedSpot == false do
        local xOff = TMC.Common.TrueRandom(3, 4)
        local yOff = TMC.Common.TrueRandom(4, 7)
        tempCoords = TMC.Natives.GetOffsetFromCoordsInDirection(coords.xyz, GetEntityHeading(PlayerPedId()), vector3(xOff, yOff, 0.0))
        if TMC.Functions.IsSpawnPointClear(tempCoords.xyz, 2) and tempCoords then
            pedCoords = vector4(tempCoords.x, tempCoords.y, tempCoords.z, GetEntityHeading(PlayerPedId()))
            foundPedSpot = true
        end
        Wait(5)
    end
    TMC.Functions.LoadModel(pedType)
    local angryOwner = CreatePed(GetPedType(pedType), pedType, pedCoords, true, true)
    while not angryOwner do
        Citizen.Wait(5)
    end
    if angryOwner ~= 0 then
        local attackpedsgroup = GetHashKey("HATES_PLAYER")
        if TMC.Common.TrueRandom(1, 100) > 60 then
            local randWeapon = TMC.Common.TrueRandom(1, #Config.Towing.attackPedWeapons)
            local weapon = Config.Towing.attackPedWeapons[randWeapon]
            SetPedDropsWeaponsWhenDead(angryOwner, false)
            GiveWeaponToPed(angryOwner, weapon, 1000, false, true) -- "487013001" is wrench
        end
        ClearPedTasksImmediately(angryOwner)
        SetEntityMaxHealth(angryOwner, 150)
        SetEntityHealth(angryOwner, 150)
        SetPedSuffersCriticalHits(angryOwner, false)
        SetPedRelationshipGroupHash(ped, attackpedsgroup)
        SetPedRelationshipGroupDefaultHash(ped, attackpedsgroup)
        TaskCombatPed(angryOwner, playerPedId, 0, 16)
        Citizen.CreateThread(function()
            while not IsPedDeadOrDying(angryOwner) do
                if #(GetEntityCoords(angryOwner) - position) > 50.0 then
                    TMC.Functions.DeleteEntity(angryOwner)
                end
                Citizen.Wait(1000)

            end

            if #(GetEntityCoords(angryOwner) - position) > 50.0 then
                TMC.Functions.DeleteEntity(angryOwner)
            end
            Citizen.Wait(1000)

            if angryOwner then
                local cachedOwner = angryOwner
                SetTimeout(300000, function()
                    TMC.Functions.DeleteEntity(cachedOwner)
                end)
            end
        end)
    end
end

function StartTow()
    if lastTowParty then
        if curTowStart ~= 'impound' then
            if remainTowStepsPerTowZone == 0 then
                if #towZonesUsed + totalTowZones > #Config.Towing.DropLocations then
                    towZonesUsed = {}
                end
                curTowZone = nil
                curTowZone = TMC.Common.TrueRandom(1, 5)
                while towZonesUsed[curTowZone] do
                    curTowZone = TMC.Common.TrueRandom(1, 5)
                    Wait(5)
                end
                towZonesUsed[curTowZone] = true
                towLocationsUsed = {}
                remainTowStepsPerTowZone = stepsPerTowZone
            end
            local isClear = false
            towCoords = nil
            hasCar = false
            towLocation = nil
            --[[temp code for testing
            tempTowZones = {}
            tempCount = 1
            for k,v in pairs(Config.Towing.DropLocations[curTowStart][6]) do
                print (v)
                tempTowZones[tempCount] = TMC.Functions.AddCircleZone('towzone', v, 25.0, {
                    useZ = false,
                    data = {
                        ['tow'] = curTowMission,
                        ['sequence'] = curTowStep,
                        ['towzone'] = v,
                    }
                })
                tempCount = tempCount + 1
            end
            --end of temp code]]

            while isClear == false do
                towLocation = TMC.Common.TrueRandom(1, #Config.Towing.DropLocations[curTowStart][curTowZone])
                while towLocationsUsed[towLocation] do
                    towLocation = TMC.Common.TrueRandom(1, #Config.Towing.DropLocations[curTowStart][curTowZone])
                    Wait(10)
                end
                towCoords = Config.Towing.DropLocations[curTowStart][curTowZone][towLocation]
                if GlobalState.VehicleTowingZonesInUse[towCoords] ~= true then
                    isClear = TMC.Functions.IsSpawnPointClear(towCoords.xyz, 3)
                end
                Wait(10)
            end

            showBlip = false
            TMC.Functions.TriggerServerEvent('jobs:towing:server:showPartyBlip', lastTowParty, towStarter)
            Wait(1000)

            if showBlip == true then
                curTowBlip = TMC.Functions.CreateBlip('Tow Point', towCoords, 123, 5, 'Tows')
                SetBlipRoute(curTowBlip, true)
                --Create polyzones around the zone where the towing will take place, when the player enters this zone it will spawn the car to avoid issues that come when cars
                --are spawned too far away.
                towPZ2 =TMC.Functions.AddCircleZone('towspawnzone', towCoords, 200.0, {
                    useZ = false,
                    data = {
                        ['tow'] = curTowMission,
                        ['sequence'] = curTowStep,
                        ['towzone'] = towCoords,
                    }
                })
            end
            TMC.Functions.TriggerServerEvent("jobs:towing:manageZoneInUse", towCoords, lastTowParty, true)

            TMC.Functions.TriggerServerEvent('parties:server:setPartyData', {
                state = 'pickup',
                hasCar = hasCar,
                carsTowed = carsTowed,
                superCount = superCount

            })
            Wait(250)
            --spawnTow()
        else
            hasCar = false
            if towingRep < 300 then
                TMC.Functions.Notify({
                    message = 'Drive around the city and look for cars that are illegally parked.  If you find one get out of your car to check if we can tow it.  If we can load it up and bring it back here.',
                    id = 'impoundzone',
                    persist = true,
                    notifType = 'info'
                })
            end
            TMC.Functions.TriggerServerEvent('parties:server:setPartyData', {
                state = 'pickup',
                hasCar = hasCar,
                carsTowed = carsTowed,
                superCount = superCount
            })
            Wait(250)
        end
    end
end

RegisterNetEvent('towing:client:createTowZone', function(towCoords)
    towPZ = TMC.Functions.AddCircleZone('towzone', towCoords, 25.0, {
        useZ = false,
        data = {
            ['tow'] = curTowMission,
            ['sequence'] = curTowStep,
            ['towzone'] = towCoords,
        }
    })
end)

RegisterNetEvent('towing:client:setInZone', function(b)
    inTowZone = b
end)

RegisterNetEvent('towing:client:setVehicles', function(curTow, curStart, towV, veh)
    if not curTowMission then
        curTowMission = curTow
    end
    if not curTowStart then
        curTowStart = curStart
    end

    if veh ~= 'na' then
        vehToTow = NetworkGetEntityFromNetworkId(veh)
    end
    towVeh = NetworkGetEntityFromNetworkId(towV)
end)

RegisterNetEvent('towing:client:setVehicleStats', function(veh, hc)
    hasCar = hc
    vehToTow = NetworkGetEntityFromNetworkId(veh)
    if curTowStart ~= 'impound' and vehToTow then
        SetVehicleDisableTowing(vehToTow, true)
        SetVehicleLivery(vehToTow, 0)
        SetVehicleEngineOn(vehToTow, false, false, false)
        SetVehicleBrake(vehToTow, false)
        SetVehicleHandbrake(vehToTow, false)
        SetVehicleUndriveable(vehToTow, true)
        SetVehicleCanBeUsedByFleeingPeds(vehToTow, false)
        SetEntityVelocity(vehToTow, 0.1, 0.1, 0.1)
        if (curTowStart == 'salvage') then
            SetVehicleDoorBroken(vehToTow, 1, true)
            SetVehicleDoorBroken(vehToTow, 0, true)
        elseif (curTowStart == 'repair') then
            local brokenDoor = TMC.Common.TrueRandom(1, 1)
            SetVehicleDoorBroken(vehToTow, brokenDoor, true)
        end
    else
        SetVehicleDisableTowing(vehToTow, true)
        SetVehicleEngineOn(vehToTow, false, false, false)
        SetVehicleBrake(vehToTow, false)
        SetVehicleHandbrake(vehToTow, false)
        SetVehicleDoorsLockedForAllPlayers(vehToTow, true)
        SetVehicleUndriveable(vehToTow, true)
        SetVehicleCanBeUsedByFleeingPeds(vehToTow, false)
        SetEntityVelocity(vehToTow, 0.1, 0.1, 0.1)
    end
end)

RegisterNetEvent('towing:client:showPrompt', function(b, towStep)
    if b == true then
        showPrompt = true
        curTowStep = towStep
        prevTowStep = towStep
    else
        showPrompt = false
    end
end)

RegisterNetEvent('towing:client:showBlip', function(b, starter)
    towStarter = starter
    if b == true then
        showBlip = true
    else
        showBlip = false
    end
end)

RegisterNetEvent('towing:client:showNotify', function(b)
    TMC.Functions.Notify({
        message = 'Make sure the driver of the tow truck gets in the car to be towed before leaving',
        id = 'partydrivernotify',
        persist = true,
        notifType = 'info'
    })
end)

RegisterNetEvent('towing:client:setStarter', function(curTow, starter)
    towStarter = starter
    curTowStart = curTow
end)

RegisterNetEvent('towing:client:setEndVariables', function()
    towStarter = nil
    curTowStart = nil
    hasCar = false
    towVeh = nil
    vehToTow = nil
    lostTowVehicle = false
    totalTowSteps = nil
    totalTowZones = nil
    stepsPerTowZone = nil
    carsTowed = 0
    superCount = 0
    remainTowStepsPerTowZone = 0
    towZonesUsed, towLocationsUsed = {}, {}
    loadTowVehicleTickThread = false
    foundSpot = false
    onTow = nil
    curTowMission = nil
end)

RegisterNetEvent('towing:client:setRestockVariables', function()
    hasCar = false
    vehToTow = nil
    lostTowVehicle = false
    towZonesUsed, towLocationsUsed = {}, {}
    loadTowVehicleTickThread = false
    foundSpot = false
end)

RegisterNetEvent('towing:client:attachVehicle', function(b, curStart, players)
    if curTowStart ~= 'impound' then
        TriggerEvent('vehiclelock:client:setLockStatus', vehToTowVin, false, 'outside', true)
        curTowStart = curStart
        local frontPos = TMC.Functions.GetFrontOffset(vehToTow)
        local rearPos = TMC.Functions.GetTrunkOffset(vehToTow)

        local lowerArm = nil

        if curTowStart == 'salvage' then
            lowerArm = 0.0
        else
            lowerArm = -1.0
        end

        local raiseArm = nil
        if curTowStart == 'salvage' then
            raiseArm = 1.0
        else
            raiseArm = 0.5
        end

        towHook = TMC.Functions.GetClosestObject({-1137425659})
        local hookCoords = GetEntityCoords(towHook)
        local carName = GetEntityModel(vehToTow)
        playerId, playerPedId, position = TMC.Functions.GetLocalData()
        TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyVehicles", lastTowParty, curTowMission, curTowStart, VehToNet(towVeh), VehToNet(vehToTow))
        Wait(1000)
        if #(GetEntityCoords(vehToTow) - position) < 10.0 and #(GetEntityCoords(towVeh) - position) < 10.0 and b == true then
            -- Attach to front
            if hookTowCount == 0 then
                hookTowCount = 1
                TMC.Functions.ProgressBar(function(complete)
                    Citizen.Wait(600)
                    SetVehicleTowTruckArmPosition(towVeh, lowerArm)
                end, 3000, 'Towing', 'Lowering Tow Truck Arm')

                Citizen.Wait(3700)

                TMC.Functions.ProgressBar(function(complete)
                    Citizen.Wait(600)

                    if #(frontPos - position) < #(rearPos - position) then
                        local hookOffsets
                        if Config.Towing.CarHookOffsets[curTowStart]['Front'][carName] then
                            hookOffsets = Config.Towing.CarHookOffsets[curTowStart]['Front'][carName]
                        end

                        SetEntityCoords(towHook, GetOffsetFromEntityInWorldCoords(vehToTow, hookOffsets.x, hookOffsets.y, hookOffsets.z), false, false, false, false)
                        hookCoords = GetEntityCoords(towHook)
                        Citizen.Wait(200)
                        rope = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        rope2 = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        AttachEntitiesToRope(rope, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_lf')
                        AttachEntitiesToRope(rope2, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_rf')

                    else
                        -- Attach to rear
                        local hookOffsets
                        for k,v in pairs(Config.Towing.CarHookOffsets[curTowStart]['Back']) do
                            if k == carName then
                                hookOffsets = v
                            end
                        end

                        SetEntityCoords(towHook, GetOffsetFromEntityInWorldCoords(vehToTow, hookOffsets.x, hookOffsets.y, hookOffsets.z), false, false, false, false)
                        hookCoords = GetEntityCoords(towHook)

                        Citizen.Wait(200)
                        rope = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        rope2 = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        AttachEntitiesToRope(rope, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_lr')
                        AttachEntitiesToRope(rope2, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_rr')
                    end
                end, 5000, 'Towing', 'Attaching car to truck')

                Citizen.Wait(5800)

                TMC.Functions.ProgressBar(function(complete)
                    Citizen.Wait(300)
                    SetVehicleTowTruckArmPosition(towVeh, raiseArm)
                end, 3000, 'Towing', 'Raising Tow Truck Arm')

                Citizen.Wait(5000)

                if players > 1 then
                    RequestEntityControl(vehToTow)
                end

                hasCar = true
                TMC.Functions.TriggerServerEvent('parties:server:setPartyData', {
                    hasCar = hasCar
                })
            end
            TMC.Functions.HidePromptGroup(towingLoadCarPrompt)
        elseif #(GetEntityCoords(vehToTow) - position) < 10.0 and #(GetEntityCoords(towVeh) - position) < 10.0 and not IsPedInVehicle(playerPedId, towVeh, false) then
            -- Attach to front
            if hookTowCount == 0 then
                hookTowCount = 1

                TMC.Functions.ProgressBar(function(complete)
                    Citizen.Wait(600)
                    local towAudio = math.random(1,4) local conf = { soundId = "tow"..towAudio, sound = "tow"..towAudio, volume = 0.12}
                    TMC.Functions.TriggerServerEvent('core_game:playSoundInArea', nil, conf, GetEntityCoords(towVeh), 20.0)
                    SetVehicleTowTruckArmPosition(towVeh, lowerArm)
                end, 3000, 'Towing', 'Lowering Tow Truck Arm')

                TMC.Functions.LoadAnimDict('mini@repair')
                TaskPlayAnim(playerPedId, 'mini@repair', 'fixing_a_ped', 1.0, -1.0, 3000, 15, 1, false, false, false)
                Citizen.Wait(3700)

                TMC.Functions.RemoveAnimDict('mini@repair')

                TMC.Functions.ProgressBar(function(complete)
                    Citizen.Wait(600)

                    if #(frontPos - position) < #(rearPos - position) then
                        local hookOffsets
                        if Config.Towing.CarHookOffsets[curTowStart]['Front'][carName] then
                            hookOffsets = Config.Towing.CarHookOffsets[curTowStart]['Front'][carName]
                        end

                        SetEntityCoords(towHook, GetOffsetFromEntityInWorldCoords(vehToTow, hookOffsets.x, hookOffsets.y, hookOffsets.z), false, false, false, false)
                        hookCoords = GetEntityCoords(towHook)

                        Citizen.Wait(200)
                        rope = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        rope2 = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        AttachEntitiesToRope(rope, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_lf')
                        AttachEntitiesToRope(rope2, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_rf')

                    else
                        -- Attach to rear
                        local hookOffsets
                        for k,v in pairs(Config.Towing.CarHookOffsets[curTowStart]['Back']) do
                            if k == carName then
                                hookOffsets = v
                            end
                        end

                        SetEntityCoords(towHook, GetOffsetFromEntityInWorldCoords(vehToTow, hookOffsets.x, hookOffsets.y, hookOffsets.z), false, false, false, false)
                        hookCoords = GetEntityCoords(towHook)

                        Citizen.Wait(200)
                        rope = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        rope2 = AddRope(hookCoords.x, hookCoords.y, hookCoords.z, 0.0, 0.0, 0.0, 0.0, 2, 0.0, 0.0, 0.0, false, false, false, 1.0, false)
                        Citizen.Wait(200)
                        AttachEntitiesToRope(rope, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_lr')
                        AttachEntitiesToRope(rope2, towHook, vehToTow, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, 'P_V_Hook_point', 'suspension_rr')
                    end
                end, 5000, 'Towing', 'Attaching car to truck')

                TMC.Functions.LoadAnimDict('anim@amb@clubhouse@tutorial@bkr_tut_ig3@')
                TaskPlayAnim(playerPedId, 'anim@amb@clubhouse@tutorial@bkr_tut_ig3@', 'machinic_loop_mechandplayer', 1.0, 1.0, 5000, 15, 1, false, true, false)
                Citizen.Wait(5500)

                StopAnimTask(playerPedId, 'anim@amb@clubhouse@tutorial@bkr_tut_ig3@', 'machinic_loop_mechandplayer')
                Citizen.Wait(300)

                TMC.Functions.RemoveAnimDict('anim@amb@clubhouse@tutorial@bkr_tut_ig3@')

                TMC.Functions.ProgressBar(function(complete)
                    Citizen.Wait(300)
                    local towAudio = math.random(1,4) local conf = { soundId = "tow"..towAudio, sound = "tow"..towAudio, volume = 0.12}
                    TMC.Functions.TriggerServerEvent('core_game:playSoundInArea', nil, conf, GetEntityCoords(towVeh), 20.0)
                    SetVehicleTowTruckArmPosition(towVeh, raiseArm)
                end, 3000, 'Towing', 'Raising Tow Truck Arm')

                TMC.Functions.LoadAnimDict('mini@repair')
                TaskPlayAnim(playerPedId, 'mini@repair', 'fixing_a_ped', 1.0, -1.0, 5000, 15, 1, false, false, false)
                Citizen.Wait(5000)

                TMC.Functions.RemoveAnimDict('mini@repair')

                if players < 2 then
                    RequestEntityControl(vehToTow)
                end

                hasCar = true
                TMC.Functions.TriggerServerEvent('parties:server:setPartyData', {
                    hasCar = hasCar
                })
            end
            TMC.Functions.HidePromptGroup(towingLoadCarPrompt)
        elseif IsPedInVehicle(playerPedId, towVeh, false) then
            TMC.Functions.SimpleNotify('Get out of the tow truck to hook up the car.', 'error')
        else
            TMC.Functions.SimpleNotify('The vehicle that needs to be towed is not close enough', 'error')
        end
        SetVehicleBrake(vehToTow, false)
        SetVehicleHandbrake(vehToTow, false)
    else
        vehToTow = closestVehicle
        oldVehToTow = closestVehicle
        prevTowStep = curTowStep
        Citizen.Wait(400)

        TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyVehicles", lastTowParty, curTowMission, curTowStart, VehToNet(towVeh), VehToNet(vehToTow))
        Wait(1000)
        for k,v in pairs(impoundSpawns) do
            if vehToTow == v or plantCar == true then
                plantCarSpawned = false
                BlipCleanFunction(vehToTow)
                isSuper = true
                break
            end
        end
        if #(GetEntityCoords(vehToTow) - position) < 10.0 and #(GetEntityCoords(towVeh) - position) < 10.0 and b == true then
            Citizen.Wait(1000)
            TMC.Functions.ProgressBar(function(finish)
                if not finish then return end
                Citizen.Wait(100)
                xoff = 0.0
                yoff = -0.85
                zoff = 1.25
                local targetDimensions = GetModelDimensions(GetEntityModel(vehToTow))
                AttachEntityToEntity(vehToTow, towVeh, GetEntityBoneIndexByName(towVeh, 'bodyshell'), 0.2, -2.85, 0.3-targetDimensions.z, 0, 0, 0, 1, 1, 0, 1, 0, 1)
                Citizen.Wait(1000)
                closestVehicle = nil
            end, 3300, "Towing", "Attaching this vehicle to the flatbed.", {canCancel = false})
            Citizen.Wait(1000)

        elseif #(GetEntityCoords(vehToTow) - position) < 10.0 and #(GetEntityCoords(towVeh) - position) < 10.0 and not IsPedInVehicle(playerPedId, towVeh, false) then
            TaskTurnPedToFaceEntity(playerPedId, vehToTow, 1000)
            Citizen.Wait(1000)
            ExecuteCommand("e mechanic")
            local towAudio = math.random(1,4) local conf = { soundId = "tow"..towAudio, sound = "tow"..towAudio, volume = 0.12}
            TMC.Functions.TriggerServerEvent('core_game:playSoundInArea', nil, conf, GetEntityCoords(towVeh), 20.0)
            RequestEntityControl(vehToTow)
            TMC.Functions.ProgressBar(function(finish)
                ExecuteCommand("e c")
                if not finish then return end
                Citizen.Wait(100)
                xoff = 0.0
                yoff = -0.85
                zoff = 1.25
                local targetDimensions = GetModelDimensions(GetEntityModel(vehToTow))
                AttachEntityToEntity(vehToTow, towVeh, GetEntityBoneIndexByName(towVeh, 'bodyshell'), 0.2, -2.85, 0.3-targetDimensions.z, 0, 0, 0, 1, 1, 0, 1, 0, 1)
                Citizen.Wait(1000)
                closestVehicle = nil
            end, 3300, "Towing", "Attaching this vehicle to the flatbed.", {canCancel = false})
            Citizen.Wait(1000)
        elseif IsPedInVehicle(playerPedId, towVeh, false) then
            TMC.Functions.SimpleNotify('Get out of the tow truck to hook up the car.', 'error')
        else
            TMC.Functions.SimpleNotify('The vehicle that needs to be towed is not close enough', 'error')
        end
        Citizen.Wait(3000)

        if IsEntityAttachedToEntity(vehToTow, towVeh) then
            hasCar = true
            justAttached = true
            if TMC.Common.TrueRandom(1, 100) > 40 and isSuper == true then
                SpawnAttackPed(GetEntityCoords(vehToTow))
            elseif TMC.Common.TrueRandom(1, 100) > 60 then
                SpawnAttackPed(GetEntityCoords(vehToTow))
            end
        end
    end
end)

RegisterNetEvent('towing:client:detachVehicle', function(b, players)
    TMC.Functions.RemoveBlip(curTowBlip)
    hasCar = false
    TMC.Functions.TriggerServerEvent('parties:server:setPartyData', {
        hasCar = hasCar
    })
    oldVehToTow = vehToTow

    local lowerArm = nil

    if curTowStart == 'salvage' then
        lowerArm = 0.0
    else
        lowerArm = -1.0
    end

    local raiseArm = nil
    if curTowStart == 'salvage' then
        raiseArm = 1.0
    else
        raiseArm = 0.5
    end

    TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyVehicles", lastTowParty, curTowMission, curTowStart, VehToNet(towVeh), VehToNet(vehToTow))
    Wait(1000)

    if curTowStart ~= 'impound' and b == true then
        TMC.Functions.ProgressBar(function(complete)
            Citizen.Wait(600)
            SetVehicleTowTruckArmPosition(towVeh, lowerArm)
        end, 3000, 'Towing', 'Lowering Tow Truck Arm')

        Citizen.Wait(3000)

        TMC.Functions.ProgressBar(function(complete)
            Citizen.Wait(600)
            DeleteRope(rope)
            DeleteRope(rope2)
            DetachVehicleFromTowTruck(towVeh, vehToTow)
        end, 2200, 'Towing', 'Detaching hook from car')

        Citizen.Wait(2200)

        TMC.Functions.ProgressBar(function(complete)
            Citizen.Wait(300)
            SetVehicleTowTruckArmPosition(towVeh, raiseArm)
        end, 3000, 'Towing', 'Raising Tow Truck Arm')

        Citizen.Wait(5000)

        vehToTow = nil
        if players > 1 then
            DoTow()
        end
    elseif curTowStart ~= 'impound' then
        TMC.Functions.ProgressBar(function(complete)
            Citizen.Wait(600)
            local towAudio = math.random(1,4) local conf = { soundId = "tow"..towAudio, sound = "tow"..towAudio, volume = 0.12}
            TMC.Functions.TriggerServerEvent('core_game:playSoundInArea', nil, conf, GetEntityCoords(towVeh), 20.0)
            SetVehicleTowTruckArmPosition(towVeh, lowerArm)
        end, 3000, 'Towing', 'Lowering Tow Truck Arm')

        TMC.Functions.LoadAnimDict('mini@repair')
        TaskPlayAnim(playerPedId, 'mini@repair', 'fixing_a_ped', 1.0, -1.0, 3000, 15, 1, false, false, false)
        Citizen.Wait(3000)

        TMC.Functions.RemoveAnimDict('mini@repair')

        TMC.Functions.ProgressBar(function(complete)
            Citizen.Wait(600)
            DeleteRope(rope)
            DeleteRope(rope2)
            DetachVehicleFromTowTruck(towVeh, vehToTow)
        end, 2200, 'Towing', 'Detaching hook from car')


        TMC.Functions.LoadAnimDict('anim@amb@clubhouse@tutorial@bkr_tut_ig3@')
        TaskPlayAnim(playerPedId, 'anim@amb@clubhouse@tutorial@bkr_tut_ig3@', 'machinic_loop_mechandplayer', 1.0, -1.0, 1800, 15, 1, false, true, false)
        Citizen.Wait(2200)

        StopAnimTask(playerPedId, 'anim@amb@clubhouse@tutorial@bkr_tut_ig3@', 'machinic_loop_mechandplayer')
        TMC.Functions.RemoveAnimDict('anim@amb@clubhouse@tutorial@bkr_tut_ig3@')


        TMC.Functions.ProgressBar(function(complete)
            Citizen.Wait(300)
            local towAudio = math.random(1,4) local conf = { soundId = "tow"..towAudio, sound = "tow"..towAudio, volume = 0.12}
            TMC.Functions.TriggerServerEvent('core_game:playSoundInArea', nil, conf, GetEntityCoords(towVeh), 20.0)
            SetVehicleTowTruckArmPosition(towVeh, raiseArm)
        end, 3000, 'Towing', 'Raising Tow Truck Arm')


        TMC.Functions.LoadAnimDict('mini@repair')
        TaskPlayAnim(playerPedId, 'mini@repair', 'fixing_a_ped', 1.0, -1.0, 3000, 15, 1, false, false, false)
        Citizen.Wait(5000)

        TMC.Functions.RemoveAnimDict('mini@repair')

        vehToTow = nil
        if players < 2 then
            DoTow()
        end
    elseif curTowStart == 'impound' and b == true then
        TMC.Functions.StopNotify('impoundzone')

        local targetDimensions = GetModelDimensions(GetEntityModel(towVeh))
        local attachedDimensions = GetModelDimensions(GetEntityModel(vehToTow))
        local dropOffY = (targetDimensions.y - 1.0) + attachedDimensions.y
        local point = GetOffsetFromEntityInWorldCoords(towVeh, 0.0, dropOffY, 0.0)
        Wait(1000)

        TMC.Functions.ProgressBar(function(finish)
            if not finish then return end
            oldVehToTow = nil
        end, 3300, "Unloading", "Unloading from flatbed.", {canCancel = false})

        Wait(3500)

        vehToTow = nil
        if players > 1 then
            DoTow()
        end
    else
        TMC.Functions.StopNotify('impoundzone')

        local targetDimensions = GetModelDimensions(GetEntityModel(towVeh))
        local attachedDimensions = GetModelDimensions(GetEntityModel(vehToTow))
        local dropOffY = (targetDimensions.y - 1.5) + attachedDimensions.y
        local point = GetOffsetFromEntityInWorldCoords(towVeh, 0.0, dropOffY, 0.0)

        ExecuteCommand("e mechanic")
        TaskTurnPedToFaceEntity(playerPedId, vehToTow, 1000)
        Wait(1000)
        local towAudio = math.random(1,4) local conf = { soundId = "tow"..towAudio, sound = "tow"..towAudio, volume = 0.12}
        TMC.Functions.TriggerServerEvent('core_game:playSoundInArea', nil, conf, GetEntityCoords(towVeh), 20.0)
        TMC.Functions.ProgressBar(function(finish)
            ExecuteCommand("e c")
            TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(oldVehToTow))
            oldVehToTow = nil
        end, 3300, "Unloading", "Unloading from flatbed.", {canCancel = false})

        Wait(3500)

        vehToTow = nil
        if players < 2 then
            DoTow()
        end
    end
end)

function towPrompt()
    if towPromptThread then return end

    Citizen.CreateThread(function()
        TMC.Functions.TriggerServerEvent('jobs:towing:server:showPartyPrompt', lastTowParty, towStarter, prevTowStep)
        Wait(1000)
        while inTowZone and hasCar == false do
            if showPrompt == true then
                TMC.Functions.ShowPromptGroup(towingLoadCarPrompt)
            end
            Citizen.Wait(300)
        end
        TMC.Functions.HidePromptGroup(towingLoadCarPrompt)
    end)
end

function DrawTowingMarker(data)
    if towingMarkerThread then return end
    Citizen.CreateThread(function()
        towingMarkerThread = true
        while inTowZone do
            DrawMarker(1, data.towzone.x, data.towzone.y, data.towzone.z - 1.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 2.0, 1.0, 14, 143, 199, 180, false, false, 2, nil, nil, nil, false)
            Citizen.Wait(5)
        end
        towingMarkerThread = false
    end)
end

function DrawReturnMarker()
    if returnMarkerThread then return end
    local returnLocation = Config.Towing.StartLocations[curTowMission].unloadingLoc
    curTowBlip = TMC.Functions.CreateBlip('Return Point', returnLocation, 123, 5, 'Tows')
    SetBlipRoute(curTowBlip, true)
    Citizen.CreateThread(function()
        returnMarkerThread = true
        while inTowZone do
            DrawMarker(1, returnLocation.x, returnLocation.y, returnLocation.z - 1.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 2.0, 1.0, 14, 143, 199, 180, false, false, 2, nil, nil, nil, false)
            Citizen.Wait(5)
        end
        returnMarkerThread = false
    end)
end

function DrawTowVehMarker(data)
    if towVehMarkerThread then return end
    curTowBlip = TMC.Functions.CreateBlip('Tow Truck', data, 123, 5, 'Tows')
    SetBlipRoute(curTowBlip, true)
    Citizen.CreateThread(function()
        towVehMarkerThread = true
        while inTowZone do
            DrawMarker(1, data.x, data.y, data.z - 1.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 2.0, 1.0, 14, 143, 199, 180, false, false, 2, nil, nil, nil, false)
            Citizen.Wait(5)
        end
        towVehMarkerThread = false
    end)
end

function removeTowedVehicle()
    if removeTowedVehicleThread then return end
    Citizen.CreateThread(function()
        removeTowedVehicleThread = true
            while oldVehToTow do
                if #(GetEntityCoords(oldVehToTow) - position) > 50.0 then
                    TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(oldVehToTow))
                    oldVehToTow = nil
                end
                Citizen.Wait(1000)
            end
            removeTowedVehicleThread = false
    end)
end

function TruckTracker()
    if truckTrackerThread then return end
    truckTrackerThread = true
    Citizen.CreateThread(function()
        while curTowMission do
            local vehpos = GetEntityCoords(towVeh)
            if #(position - vehpos) > 150.0 then
                DoTowCleanup('abandoned')
            end
            Citizen.Wait(10000)
        end
        truckTrackerThread = false
    end)
end

function LoadTowVehicleTick()
    if loadTowVehicleTickThread then return end
    Citizen.CreateThread(function()
        loadTowVehicleTickThread = true
        StartTow()
        loadTowVehicleTickThread = false
    end)
end

function spawnTow()
    TMC.Functions.StopNotify('towzone')
    local forward = GetEntityForwardVector(playerPedId)
    local x, y, z = table.unpack(position + forward * 0.5)
    local config = Config.Towing
    local vehTypeNum  = TMC.Common.TrueRandom(1, #config.vehToTowTypes[curTowStart])
    towAssigned = true
    vehToTow = nil
    vehToTow = TMC.Functions.SpawnVehicle(config.vehToTowTypes[curTowStart][vehTypeNum], towCoords, true)
    while not DoesEntityExist(towVeh) do
        Citizen.Wait(0)
    end

    vehToTowVin = TMC.Common.GenerateVIN(true)

    TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyVehicles", lastTowParty, curTowMission, curTowStart, VehToNet(towVeh), VehToNet(vehToTow)) -- change vehtotow for impound
    Citizen.Wait(1000)
    TMC.Functions.TriggerServerEvent("jobs:towing:server:setPartyVehicleStats", lastTowParty, VehToNet(vehToTow), vehToTowVin, hasCar)
    Citizen.Wait(1000)
    TMC.Functions.RemoveZoneById(towPZ2.id)
end

function DoTow()
    local forward = GetEntityForwardVector(playerPedId)
    local x, y, z = table.unpack(position + forward * 0.5)
    towCoords = vector3(x, y, z - 1.0)
    TMC.Functions.SimpleNotify('Car delivered', 'success')
    towAssigned = false
    removeTowedVehicle()
    towRepCounter = towRepCounter + 1
    if towRepCounter == 2 then
        local towChance = TMC.Common.TrueRandom(1, 100)
        if (towChance > 25) then
            TMC.Functions.TriggerServerEvent('jobs:towing:towRepIncrease', lastTowParty)
        end
        Wait(500)
        TMC.Functions.SimpleNotify('You feel like you\'re getting better at this', 'success', 4500)
        towRepCounter = 0
    end
    Wait(500)
    curTowStep = curTowStep + 1
    SetBlipRoute(curTowBlip, false)
    TMC.Functions.RemoveBlip(curTowBlip)
    if isSuper == true then
        superCount = superCount + 1;
    end
    carsTowed = carsTowed + 1;
    TMC.Functions.TriggerServerEvent('parties:server:setPartyData', {
        hasCar = hasCar,
        carsTowed = carsTowed,
        superCount = superCount
    })
    Wait(300)
    --TMC.Functions.TriggerServerEvent('jobs:towing:addTowPay', curTowMission, isSuper)
    isSuper = false
    if curTowStep == totalTowSteps then
        DoTowCleanup('complete')
    else
        curTowBlip = nil
        remainTowStepsPerTowZone = remainTowStepsPerTowZone - 1
        StartTow(curTowMission)
    end
end

function DoTowCleanup(type)
    curTowStep, totalTowSteps, curTowZone = 0, nil, nil
    TMC.Functions.TriggerServerEvent("jobs:towing:manageZoneInUse", impoundSpawnCoords, lastTowParty,  false)
    if type == 'complete' then
        BlipCleanFunction()
        TMC.Functions.StopNotify('impoundzone')
        TMC.Functions.SimpleNotify('Return to the scrap yard to pick up additional tow orders or end your route', 'info', 10000)
        TMC.Functions.TriggerServerEvent('parties:server:setPartyData', 'state', 'finished')
        SetBlipRoute(createdTowBlips[curTowMission], true)
        TMC.Functions.TriggerServerEvent("jobs:towing:removePartyImpoundZones", lastTowParty)
        foundSpot = false
        towStarter = nil
        hasCar = false
    elseif type == 'end' then
        BlipCleanFunction()
        TMC.Functions.StopNotify('impoundzone')
        TMC.Functions.SimpleNotify('Thanks for your work, here\'s your paycheck', 'success', 7500)
        TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(towVeh))
        TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(vehToTow))
        TMC.Functions.TriggerServerEvent('jobs:towing:towPayout', lostTowVehicle, curTowMission, lastTowParty)
        TMC.Functions.TriggerServerEvent('parties:server:setPartyData', 'state', 'end')
        SetBlipRoute(createdTowBlips[curTowMission], false)
        curTowMission, onTow = nil, nil
        remainTowStepsPerTowZone = 0
        towZonesUsed, towLocationsUsed = {}, {}
        loadTowVehicleTickThread = false
        TMC.Functions.RemoveBlip(curTowBlip)
        TMC.Functions.RemoveBlip(returnTowBlip)
        TMC.Functions.TriggerServerEvent("jobs:towing:removePartyImpoundZones", lastTowParty)
        foundSpot = false
        towStarter = nil
        if lastTowParty then
            TMC.Functions.TriggerServerEvent('parties:server:leaveParty')
        end
    elseif type == 'abandoned' then
        BlipCleanFunction()
        TMC.Functions.StopNotify('impoundzone')
        TMC.Functions.SimpleNotify('You have abandoned your towing duties and have lost your vehicle deposit', 'error')
        TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(towVeh))
        TMC.Functions.TriggerServerEvent('TMC:RequestVehicleDelete', VehToNet(vehToTow))
        towZonesUsed, towLocationsUsed = {}, {}
        SetBlipRoute(createdTowBlips[curTowMission], false)
        SetBlipRoute(curTowBlip, false)
        TMC.Functions.TriggerServerEvent('parties:server:setPartyData', 'state', 'end')
        TMC.Functions.TriggerServerEvent('jobs:towing:towPayout', lostTowVehicle, curTowMission, lastTowParty)
        TMC.Functions.TriggerServerEvent("jobs:towing:removePartyImpoundZones", lastTowParty)
        TMC.Functions.RemoveBlip(curTowBlip)
        TMC.Functions.RemoveBlip(returnTowBlip)
        foundSpot = false
        towStarter = nil
        if lastTowParty then
            TMC.Functions.TriggerServerEvent('parties:server:leaveParty')
        end
    end
end

function TowPartyCleanup(clearPartyInfo, partyId)
    if clearPartyInfo then
        for k,v in pairs(impoundPZs) do
            TMC.Functions.RemoveZoneById(impoundPZs[k].id)
        end
        RemoveStateBagChangeHandler('Party:'..tostring(partyId))
    end
end

function BlipCleanFunction(vehicle)
    if vehicle ~= nil then
        for k,v in pairs(blipTable) do
            if v.vehicle == vehicle then
                RemoveBlip(v.blip)
                blipTable[k] = nil
            end
        end
    else
        for k,v in pairs(blipTable) do
            RemoveBlip(v.blip)
            blipTable[k] = nil
        end
    end
end

function HandleTowingBlips()
    for k,v in pairs(Config.Towing.StartLocations) do
        if towingRep >= v.RepReq and not createdTowBlips[k] then
            createdTowBlips[k] = TMC.Functions.CreateBlip(v.Type .. ' Towing', v.Location.xyz, 637, 4, 'Towing Job')
        end
    end
end

Citizen.CreateThread(function()
    AddStateBagChangeHandler('rep', string.format('player:%s', GetPlayerServerId(playerId)), function(bag, key, value)
        if value ~= nil and value['towing'] then
            towingRep = value['towing']
            HandleTowingBlips()
        end
    end)
end)

RegisterNetEvent('TMC:Client:OnPlayerUnload', function()
    for _,v in pairs(createdTowBlips) do
        TMC.Functions.RemoveBlip(v)
    end
    createdTowBlips, towLocationsUsed, towZonesUsed, impoundSpawns, impoundSpawnZones, impoundSpawnCoords, impoundPZs = {}, {}, {}, {}, {}, {}, {}
    towingRep, totalTowSteps, totalTowZones, stepsPerTowZone, remainTowStepsPerTowZone, curTowStep, prevTowStep, curTowMission, curTowZone, curTowBlip, towItem, onTow, towRepCounter, lostTowVehicle, returnTowBlip = nil, nil, nil, nil, nil, 0, 0, nil, nil, nil, nil, false, 0, false, nil
    towVeh, towVin, vehToTow, vehToTowVin, oldVehToTow, vehToSpawn, hasCar, towHook, rope, rope2, isSuper = nil, nil, nil, nil, nil, false, nil, nil, nil, nil
    towingStartPrompt, towingEndPrompt, towingDropPrompt, towLoadingZonePrompt = nil, nil, nil, nil
    curTowStart, towAssigned, towPZ, towPZ2, truckPZ, firstTow = nil, nil, nil, nil, nil, nil
    towingMarkerThread, returnMarkerThread, truckTrackerThread, loadTowVehicleTickThread, inTowZone = false, false, false, false
    local playerId, playerPedId, position, towCoords, doTowCount, impoundCount, hookTowCount, closestVehicle = nil, nil, nil, nil, 0, 0, 0, nil
end)

AddEventHandler('TMC:UpdatePlayerPedId', function(data) playerPedId = data; end)
AddEventHandler('TMC:UpdatePosition', function(data) position = data; end)
